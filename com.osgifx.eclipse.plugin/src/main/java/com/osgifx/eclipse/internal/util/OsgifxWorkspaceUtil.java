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
package com.osgifx.eclipse.internal.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class OsgifxWorkspaceUtil {

    private static final String PLUGIN_ID = "com.osgifx.eclipse.plugin";

    private OsgifxWorkspaceUtil() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static void deleteDirectory(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    public static File getLogsLocation() {
        final File logs = new File(getStateLocation(), "logs");
        if (!logs.exists()) {
            logs.mkdirs();
        }
        return logs;
    }

    public static File getStateLocation() {
        final Bundle bundle = FrameworkUtil.getBundle(OsgifxWorkspaceUtil.class);
        if (bundle != null) {
            final IPath state = Platform.getStateLocation(bundle);
            if (state != null) {
                return state.toFile();
            }
        }
        // Fallback for non-OSGi or if Platform is not available
        final String userHome = System.getProperty("user.home");
        return new File(userHome, ".osgifx-eclipse");
    }

    public static IPreferenceStore getPreferenceStore() {
        return new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
    }
}
