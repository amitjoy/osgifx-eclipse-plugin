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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang.SystemUtils;

import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class RunOsgiFxDownloader {

    private static final String SCRIPT_URL      = "https://raw.githubusercontent.com/amitjoy/osgifx/main/scripts/RunOSGiFx";
    private static final String SCRIPT_FILENAME = "RunOSGiFx";

    public Path getScriptPath() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        return stateLocation.toPath().resolve(SCRIPT_FILENAME);
    }

    public boolean isScriptAvailable() {
        return Files.exists(getScriptPath());
    }

    public void download() throws IOException {
        final var scriptPath = getScriptPath();
        scriptPath.getParent().toFile().mkdirs();

        try (final var is = new BufferedInputStream(new URL(SCRIPT_URL).openStream())) {
            Files.copy(is, scriptPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Set executable permission on Unix-like systems
        if (!SystemUtils.IS_OS_WINDOWS) {
            scriptPath.toFile().setExecutable(true);
        }
    }
}
