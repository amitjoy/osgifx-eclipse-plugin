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
package com.osgifx.eclipse.internal.preferences;

import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.AUTO_MANAGE_RUNTIME;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.DEFAULT_AUTO_MANAGE;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.DEFAULT_GAV;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.DEFAULT_USE_LOCAL;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.OSGIFX_GAV;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.USE_LOCAL_JAR;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;

import com.osgifx.eclipse.internal.Activator;

public final class OsgifxPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        final var node = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);

        node.putBoolean(AUTO_MANAGE_RUNTIME, DEFAULT_AUTO_MANAGE);
        node.putBoolean(USE_LOCAL_JAR, DEFAULT_USE_LOCAL);
        node.put(OSGIFX_GAV, DEFAULT_GAV);
    }

}
