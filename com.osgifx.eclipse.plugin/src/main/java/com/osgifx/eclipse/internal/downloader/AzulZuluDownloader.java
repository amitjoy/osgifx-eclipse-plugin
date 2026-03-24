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
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.osgifx.eclipse.internal.util.OSUtils;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class AzulZuluDownloader extends Job {

    private static final String API_URL     = "https://api.azul.com/metadata/v1/zulu/packages";
    private static final String VERSION     = "25";
    private static final String ARCHIVE_DIR = "zulu-fx-25";

    private static final Object DOWNLOAD_LOCK = new Object();

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
        if (!Files.exists(runtimePath)) {
            return null;
        }
        final var javaExe = OSUtils.IS_OS_WINDOWS ? "java.exe" : "java";

        try (final var stream = Files.walk(runtimePath)) {
            return stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().equals(javaExe))
                    .filter(path -> path.getParent().getFileName().toString().equals("bin")).findFirst().orElse(null);
        } catch (final IOException e) {
            return null;
        }
    }

    public boolean isRuntimeAvailable() {
        final var path = getJavaExecutablePath();
        return path != null && Files.exists(path);
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        synchronized (DOWNLOAD_LOCK) {
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

                try {
                    monitor.subTask("Extracting runtime archive...");
                    extractArchive(archiveFile, runtimePath.toFile());

                    monitor.subTask("Validating JavaFX modules...");
                    validateJavaFx();
                } finally {
                    Files.deleteIfExists(archiveFile.toPath());
                }

                return Status.OK_STATUS;
            } catch (final Exception e) {
                return errorStatus("Failed to download Azul Zulu FX 25: " + e.getMessage());
            } finally {
                monitor.done();
            }
        }
    }

    private String fetchDownloadUrl() throws IOException {
        final var os   = detectOs();
        final var arch = detectArch();
        final var url  = new URL(API_URL + "?java_version=" + VERSION + "&os=" + os + "&arch=" + arch
                + "&archive_type=zip&java_package_type=jdk&javafx_bundled=true&latest=true");

        try (final var is = url.openStream()) {
            final var response = new String(is.readAllBytes());
            final var packages = gson.fromJson(response, JsonArray.class);

            for (final var element : packages) {
                final var pkg = element.getAsJsonObject();
                if (pkg.has("download_url")) {
                    return pkg.get("download_url").getAsString();
                }
            }
        }
        return null;
    }

    private String detectOs() {
        if (OSUtils.IS_OS_WINDOWS) {
            return "windows";
        } else if (OSUtils.IS_OS_MAC) {
            return "macos";
        } else {
            return "linux";
        }
    }

    private String detectArch() {
        final var arch = OSUtils.OS_ARCH.toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm")) {
            return "arm";
        }
        if (arch.contains("64")) {
            return "x86_64"; // Safer mapping for Azul API
        }
        return "x86";
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
        } catch (final IOException e) {
            Files.deleteIfExists(tempFile.toPath());
            throw e;
        }

        return tempFile;
    }

    private void extractArchive(final File archive, final File targetDir) throws IOException {
        if (targetDir.exists()) {
            OsgifxWorkspaceUtil.deleteDirectory(targetDir.toPath());
        }
        targetDir.mkdirs();

        try (final var fis = new FileInputStream(archive); final var zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final var entryPath  = entry.getName();
                final var targetFile = new File(targetDir, entryPath);

                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();

                    // Prevent Zip Slip Vulnerability
                    if (!targetFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath() + File.separator)) {
                        throw new IOException("Zip entry is outside of the target dir: " + entryPath);
                    }

                    Files.copy(zis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Set executable permission on Unix-like systems for bin binaries
                    if (!OSUtils.IS_OS_WINDOWS && (entryPath.contains("/bin/") || entryPath.startsWith("bin/"))) {
                        targetFile.setExecutable(true);
                    }
                }
            }
        }
    }

    private void validateJavaFx() throws Exception {
        final var javaExe = getJavaExecutablePath();
        if (javaExe == null || !Files.exists(javaExe)) {
            throw new Exception("Java executable not found in extracted runtime");
        }

        final var cmd = new ArrayList<String>();
        cmd.add(javaExe.toString());
        if (OSUtils.IS_OS_MAC) {
            cmd.add("-Djdk.lang.Process.launchMechanism=FORK");
        }
        cmd.add("--list-modules");

        final var pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        final var process = pb.start();
        try (final var is = process.getInputStream()) {
            final var output = new String(is.readAllBytes());
            process.waitFor();
            if (!output.contains("javafx.controls")) {
                throw new Exception("Downloaded runtime does not contain JavaFX modules");
            }
            if (!output.contains("jdk.compiler")) {
                throw new Exception("Downloaded runtime does not contain the Java compiler (jdk.compiler) required to launch the script");
            }
        }
    }

    private IStatus errorStatus(final String message) {
        return new Status(IStatus.ERROR, "com.osgifx.eclipse.plugin", message);
    }
}
