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

import static com.osgifx.eclipse.internal.util.Constants.SCRIPT_FILENAME;
import static com.osgifx.eclipse.internal.util.Constants.SCRIPT_URL;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.runtime.IStatus;

import com.osgifx.eclipse.internal.Activator;
import com.osgifx.eclipse.internal.util.OSUtils;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class RunOsgiFxDownloader {

    private static final Object DOWNLOAD_LOCK = new Object();

    /**
     * Returns the path where the RunOSGiFx script is stored.
     *
     * @return the script file path
     */
    public Path getScriptPath() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        return stateLocation.toPath().resolve(SCRIPT_FILENAME);
    }

    /**
     * Checks if the RunOSGiFx script has been downloaded.
     *
     * @return {@code true} if the script exists, {@code false} otherwise
     */
    public boolean isScriptAvailable() {
        return Files.exists(getScriptPath());
    }

    /**
     * Downloads the RunOSGiFx script from the remote repository.
     * If the script is already cached, this method returns immediately.
     *
     * @throws IOException if the download fails
     */
    public void download() throws IOException {
        synchronized (DOWNLOAD_LOCK) {
            if (isScriptAvailable()) {
                Activator.log(IStatus.INFO, "RunOSGiFx script already cached at: " + getScriptPath(), null);
                return;
            }

            final var scriptPath = getScriptPath();
            scriptPath.getParent().toFile().mkdirs();

            Activator.log(IStatus.INFO, "Downloading RunOSGiFx script from: " + SCRIPT_URL, null);
            try (final var is = new BufferedInputStream(new URL(SCRIPT_URL).openStream())) {
                Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                Activator.log(IStatus.ERROR, "Failed to download RunOSGiFx script from: " + SCRIPT_URL, e);
                throw e;
            }

            // Set executable permission on Unix-like systems
            if (!OSUtils.IS_OS_WINDOWS) {
                scriptPath.toFile().setExecutable(true);
            }
            Activator.log(IStatus.INFO, "RunOSGiFx script downloaded successfully to: " + scriptPath, null);
        }
    }
}
