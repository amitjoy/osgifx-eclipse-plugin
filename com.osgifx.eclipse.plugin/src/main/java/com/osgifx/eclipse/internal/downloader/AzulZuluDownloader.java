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

import static com.osgifx.eclipse.internal.util.Constants.ARCHIVE_TYPE_ZIP;
import static com.osgifx.eclipse.internal.util.Constants.ARCH_ARM;
import static com.osgifx.eclipse.internal.util.Constants.ARCH_X86;
import static com.osgifx.eclipse.internal.util.Constants.ARCH_X86_64;
import static com.osgifx.eclipse.internal.util.Constants.AZUL_API_URL;
import static com.osgifx.eclipse.internal.util.Constants.BIN_DIR;
import static com.osgifx.eclipse.internal.util.Constants.BUFFER_SIZE;
import static com.osgifx.eclipse.internal.util.Constants.JAVAFX_CONTROLS_MODULE;
import static com.osgifx.eclipse.internal.util.Constants.JAVA_EXE_UNIX;
import static com.osgifx.eclipse.internal.util.Constants.JAVA_EXE_WINDOWS;
import static com.osgifx.eclipse.internal.util.Constants.JAVA_PACKAGE_TYPE;
import static com.osgifx.eclipse.internal.util.Constants.JAVA_VERSION;
import static com.osgifx.eclipse.internal.util.Constants.JDK_COMPILER_MODULE;
import static com.osgifx.eclipse.internal.util.Constants.JVM_ARG_LIST_MODULES;
import static com.osgifx.eclipse.internal.util.Constants.JVM_ARG_MAC_FORK;
import static com.osgifx.eclipse.internal.util.Constants.OS_LINUX;
import static com.osgifx.eclipse.internal.util.Constants.OS_MACOS;
import static com.osgifx.eclipse.internal.util.Constants.OS_WINDOWS;
import static com.osgifx.eclipse.internal.util.Constants.TEMP_FILE_PREFIX;
import static com.osgifx.eclipse.internal.util.Constants.TEMP_FILE_SUFFIX;
import static com.osgifx.eclipse.internal.util.Constants.ZULU_ARCHIVE_DIR;

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
import com.osgifx.eclipse.internal.Activator;
import com.osgifx.eclipse.internal.util.OSUtils;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class AzulZuluDownloader extends Job {

    private static final Object DOWNLOAD_LOCK = new Object();

    private final Gson gson = new Gson();

    /**
     * Creates a new Azul Zulu FX 25 downloader job.
     */
    public AzulZuluDownloader() {
        super("Downloading Azul Zulu FX 25 Runtime");
    }

    /**
     * Returns the path where the Azul Zulu FX 25 runtime is stored.
     *
     * @return the runtime directory path
     */
    public Path getRuntimePath() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        return stateLocation.toPath().resolve(ZULU_ARCHIVE_DIR);
    }

    /**
     * Locates the Java executable within the downloaded runtime.
     *
     * @return the path to the java executable, or {@code null} if not found
     */
    public Path getJavaExecutablePath() {
        final var runtimePath = getRuntimePath();
        if (!Files.exists(runtimePath)) {
            return null;
        }
        final var javaExe = OSUtils.IS_OS_WINDOWS ? JAVA_EXE_WINDOWS : JAVA_EXE_UNIX;

        try (final var stream = Files.walk(runtimePath, 7)) {
            return stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().equals(javaExe))
                    .filter(path -> path.getParent().getFileName().toString().equals(BIN_DIR)).findFirst().orElse(null);
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Checks if the Azul Zulu FX 25 runtime is available and ready to use.
     *
     * @return {@code true} if the runtime is available, {@code false} otherwise
     */
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
                    Activator.log(IStatus.INFO, "Azul Zulu FX 25 runtime already available at: " + runtimePath, null);
                    return Status.OK_STATUS;
                }

                monitor.beginTask("Downloading Azul Zulu FX 25", IProgressMonitor.UNKNOWN);

                monitor.subTask("Querying Azul API for download URL...");
                Activator.log(IStatus.INFO, "Querying Azul API for Java 25 FX bundle...", null);
                final var downloadUrl = fetchDownloadUrl();
                if (downloadUrl == null) {
                    return errorStatus("Failed to find Java 25 FX bundle for your platform");
                }
                Activator.log(IStatus.INFO, "Azul Zulu FX 25 download URL resolved: " + downloadUrl, null);

                monitor.subTask("Downloading runtime archive...");
                Activator.log(IStatus.INFO, "Downloading Azul Zulu FX 25 runtime archive...", null);
                final var archiveFile = downloadArchive(downloadUrl, monitor);

                try {
                    monitor.subTask("Extracting runtime archive...");
                    Activator.log(IStatus.INFO, "Extracting Azul Zulu FX 25 runtime to: " + runtimePath, null);
                    extractArchive(archiveFile, runtimePath.toFile());

                    monitor.subTask("Validating JavaFX modules...");
                    validateJavaFx();
                    Activator.log(IStatus.INFO, "Azul Zulu FX 25 runtime validated successfully at: " + runtimePath,
                            null);
                } finally {
                    Files.deleteIfExists(archiveFile.toPath());
                }

                return Status.OK_STATUS;
            } catch (final Exception e) {
                return errorStatus("Failed to download Azul Zulu FX 25: " + e.getMessage(), e);
            } finally {
                monitor.done();
            }
        }
    }

    private String fetchDownloadUrl() throws IOException {
        final var os   = detectOs();
        final var arch = detectArch();
        final var url  = new URL(AZUL_API_URL + "?java_version=" + JAVA_VERSION + "&os=" + os + "&arch=" + arch
                + "&archive_type=" + ARCHIVE_TYPE_ZIP + "&java_package_type=" + JAVA_PACKAGE_TYPE
                + "&javafx_bundled=true&latest=true");

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
            return OS_WINDOWS;
        } else if (OSUtils.IS_OS_MAC) {
            return OS_MACOS;
        } else {
            return OS_LINUX;
        }
    }

    private String detectArch() {
        final var arch = OSUtils.OS_ARCH.toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm")) {
            return ARCH_ARM;
        }
        if (arch.contains("64")) {
            return ARCH_X86_64;
        }
        return ARCH_X86;
    }

    private File downloadArchive(final String url, final IProgressMonitor monitor) throws IOException {
        final var connection    = new URL(url).openConnection();
        final var contentLength = connection.getContentLengthLong();

        final var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX).toFile();

        try (final var is = new BufferedInputStream(connection.getInputStream());
                final var os = new FileOutputStream(tempFile)) {

            final var buffer    = new byte[BUFFER_SIZE];
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
                    final var canonicalTarget = targetDir.getCanonicalPath();
                    final var canonicalFile   = targetFile.getCanonicalPath();
                    if (!canonicalFile.startsWith(canonicalTarget + File.separator)) {
                        throw new ZipSlipException(entryPath, canonicalTarget, canonicalFile);
                    }

                    Files.copy(zis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Set executable permission on Unix-like systems for bin binaries
                    if (!OSUtils.IS_OS_WINDOWS
                            && (entryPath.contains("/" + BIN_DIR + "/") || entryPath.startsWith(BIN_DIR + "/"))) {
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
            cmd.add(JVM_ARG_MAC_FORK);
        }
        cmd.add(JVM_ARG_LIST_MODULES);

        final var pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        final var process = pb.start();
        try (final var is = process.getInputStream()) {
            final var output = new String(is.readAllBytes());
            process.waitFor();
            if (!output.contains(JAVAFX_CONTROLS_MODULE)) {
                throw new Exception("Downloaded runtime does not contain JavaFX modules");
            }
            if (!output.contains(JDK_COMPILER_MODULE)) {
                throw new Exception("Downloaded runtime does not contain the Java compiler (jdk.compiler) required to launch the script");
            }
        }
    }

    private IStatus errorStatus(final String message) {
        return errorStatus(message, null);
    }

    private IStatus errorStatus(final String message, final Throwable throwable) {
        Activator.log(IStatus.ERROR, message, throwable);
        return new Status(IStatus.ERROR, "com.osgifx.eclipse.plugin", message, throwable);
    }
}
