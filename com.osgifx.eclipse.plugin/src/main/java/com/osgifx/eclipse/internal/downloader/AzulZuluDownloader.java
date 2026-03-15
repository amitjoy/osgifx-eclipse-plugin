/*******************************************************************************
 * Copyright 2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.eclipse.internal.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class AzulZuluDownloader extends Job {

    private static final String API_URL     = "https://api.azul.com/metadata/v1/zulu/packages";
    private static final String VERSION     = "25";
    private static final String ARCHIVE_DIR = "zulu-fx-25";

    private final Gson gson = new Gson();

    public AzulZuluDownloader() {
        super("Downloading Azul Zulu FX 25 Runtime");
    }

    public Path getRuntimePath() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        return stateLocation.toPath().resolve(ARCHIVE_DIR);
    }

    public Path getJavaExecutablePath() {
        final var runtimePath = getRuntimePath();
        final var binDir      = runtimePath.resolve("bin");
        final var javaExe     = SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java";
        return binDir.resolve(javaExe);
    }

    public boolean isRuntimeAvailable() {
        return Files.exists(getJavaExecutablePath());
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        try {
            final var runtimePath = getRuntimePath();
            if (isRuntimeAvailable()) {
                return Status.OK_STATUS;
            }

            monitor.beginTask("Downloading Azul Zulu FX 25", IProgressMonitor.UNKNOWN);

            monitor.subTask("Querying Azul API for download URL...");
            final var downloadUrl = fetchDownloadUrl();
            if (downloadUrl == null) {
                return errorStatus("Failed to find Java 25 FX bundle for your platform");
            }

            monitor.subTask("Downloading runtime archive...");
            final var archiveFile = downloadArchive(downloadUrl, monitor);

            monitor.subTask("Extracting runtime archive...");
            extractArchive(archiveFile, runtimePath.toFile());

            Files.deleteIfExists(archiveFile.toPath());

            monitor.subTask("Validating JavaFX modules...");
            if (!validateJavaFx()) {
                return errorStatus("Downloaded runtime does not contain JavaFX modules");
            }

            return Status.OK_STATUS;
        } catch (final Exception e) {
            return errorStatus("Failed to download Azul Zulu FX 25: " + e.getMessage());
        } finally {
            monitor.done();
        }
    }

    private String fetchDownloadUrl() throws IOException {
        final var os   = detectOs();
        final var arch = detectArch();
        final var url  = new URL(API_URL + "?java_version=" + VERSION + "&os=" + os + "&arch=" + arch
                + "&archive_type=dmg&java_package_type=jre&javafx=true&latest=true");

        try (final var is = url.openStream()) {
            final var response = new String(is.readAllBytes());
            final var packages = gson.fromJson(response, JsonArray.class);

            for (final var element : packages) {
                final var pkg       = element.getAsJsonObject();
                final var downloads = pkg.getAsJsonArray("downloads");
                if (downloads != null && !downloads.isEmpty()) {
                    final var download = downloads.get(0).getAsJsonObject();
                    return download.get("url").getAsString();
                }
            }
        }
        return null;
    }

    private String detectOs() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows";
        } else if (SystemUtils.IS_OS_MAC) {
            return "macos";
        } else {
            return "linux";
        }
    }

    private String detectArch() {
        final var arch = SystemUtils.OS_ARCH.toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm")) {
            return "arm";
        }
        return "x64";
    }

    private File downloadArchive(final String url, final IProgressMonitor monitor) throws IOException {
        final var connection    = new URL(url).openConnection();
        final var contentLength = connection.getContentLengthLong();

        final var tempFile = Files.createTempFile("zulu-fx-25", ".zip").toFile();

        try (final var is = new BufferedInputStream(connection.getInputStream());
                final var os = new FileOutputStream(tempFile)) {

            final var buffer    = new byte[8192];
            var       totalRead = 0L;
            var       bytesRead = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (contentLength > 0) {
                    final var progress = (int) ((totalRead * 100) / contentLength);
                    monitor.subTask("Downloading... " + progress + "%");
                }
            }
        }

        return tempFile;
    }

    private void extractArchive(final File archive, final File targetDir) throws IOException {
        targetDir.mkdirs();

        try (final var fis = new FileInputStream(archive); final var zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final var entryPath = entry.getName();
                // Skip the root directory in the archive
                final var slashIndex   = entryPath.indexOf('/');
                final var relativePath = slashIndex >= 0 ? entryPath.substring(slashIndex + 1) : entryPath;

                if (relativePath.isEmpty()) {
                    continue;
                }

                final var targetFile = new File(targetDir, relativePath);

                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();
                    Files.copy(zis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Set executable permission on Unix-like systems
                    if (!SystemUtils.IS_OS_WINDOWS && relativePath.startsWith("bin/")) {
                        targetFile.setExecutable(true);
                    }
                }
            }
        }
    }

    private boolean validateJavaFx() throws IOException, InterruptedException {
        final var javaExe = getJavaExecutablePath();
        if (!Files.exists(javaExe)) {
            return false;
        }

        final var pb = new ProcessBuilder(javaExe.toString(), "--list-modules");
        pb.redirectErrorStream(true);

        final var process = pb.start();
        try (final var is = process.getInputStream()) {
            final var output = new String(is.readAllBytes());
            process.waitFor();
            return output.contains("javafx.controls");
        }
    }

    private IStatus errorStatus(final String message) {
        return new Status(IStatus.ERROR, "com.osgifx.eclipse.plugin", message);
    }
}
