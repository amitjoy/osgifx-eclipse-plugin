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

public interface OsgifxPreferenceConstants {

    String AUTO_MANAGE_RUNTIME = "autoManageRuntime";
    String CUSTOM_JAVA_PATH    = "customJavaPath";
    String OSGIFX_GAV          = "osgifxGav";
    String OSGIFX_LOCAL_JAR    = "osgifxLocalJar";
    String USE_LOCAL_JAR       = "useLocalJar";
    String DOWNLOAD_CACHE_DIR  = "downloadCacheDir";

    // Defaults
    boolean DEFAULT_AUTO_MANAGE = true;
    boolean DEFAULT_USE_LOCAL   = false;
    String  DEFAULT_GAV         = "com.osgifx:osgifx:latest";
}
