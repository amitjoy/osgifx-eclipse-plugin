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
package com.osgifx.eclipse.internal;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;

public final class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.osgifx.eclipse.plugin";

    private static Activator instance;
    private BundleContext    bundleContext;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        instance      = this;
        bundleContext = context;
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        instance      = null;
        bundleContext = null;
        super.stop(context);
    }

    public static Activator getInstance() {
        return instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public static Preferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    }

    public static void log(final int severity, final String message, final Throwable throwable) {
        if (instance != null) {
            instance.getLog().log(new Status(severity, PLUGIN_ID, message, throwable));
        }
    }
}
