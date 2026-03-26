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

import java.io.IOException;

/**
 * Exception thrown when a Zip Slip vulnerability is detected during archive extraction.
 * This occurs when a malicious ZIP file contains entries with path traversal sequences
 * (e.g., "../../../") that would extract files outside the intended target directory.
 */
public final class ZipSlipException extends IOException {

    private static final long serialVersionUID = 1L;

    private final String entryPath;
    private final String targetDirectory;
    private final String resolvedPath;

    public ZipSlipException(final String entryPath, final String targetDirectory, final String resolvedPath) {
        super(String.format(
                "Security violation: Zip entry '%s' attempts to write outside target directory. "
                        + "Target: '%s', Resolved: '%s'. This may indicate a malicious archive.",
                entryPath, targetDirectory, resolvedPath));
        this.entryPath       = entryPath;
        this.targetDirectory = targetDirectory;
        this.resolvedPath    = resolvedPath;
    }

    public String getEntryPath() {
        return entryPath;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public String getResolvedPath() {
        return resolvedPath;
    }
}
