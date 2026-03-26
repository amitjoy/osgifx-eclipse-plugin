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

public final class Constants {

    private Constants() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    // Maven Central
    public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";

    // Agent Artifact
    public static final String AGENT_GROUP_ID    = "com.osgifx";
    public static final String AGENT_ARTIFACT_ID = "com.osgifx.console.agent";

    // Azul Zulu
    public static final String AZUL_API_URL     = "https://api.azul.com/metadata/v1/zulu/packages";
    public static final String JAVA_VERSION     = "25";
    public static final String ZULU_ARCHIVE_DIR = "zulu-fx-25";
    public static final String JAVA_EXE_WINDOWS = "java.exe";
    public static final String JAVA_EXE_UNIX    = "java";
    public static final String BIN_DIR          = "bin";

    // RunOSGiFx Script
    public static final String SCRIPT_URL      = "https://raw.githubusercontent.com/amitjoy/osgifx/main/scripts/RunOSGiFx";
    public static final String SCRIPT_FILENAME = "RunOSGiFx";

    // Connection Types
    public static final String CONNECTION_TYPE_SOCKET = "SOCKET";
    public static final String CONNECTION_TYPE_MQTT   = "MQTT";

    // Connection Status
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_NEVER   = "NEVER";

    // File Names
    public static final String CONNECTIONS_FILE = "connections.json";
    public static final String LOGS_DIR         = "logs";

    // Process Launch Constants
    public static final long STARTUP_CHECK_INTERVAL_MS = 500L;
    public static final int  STARTUP_CHECK_ITERATIONS  = 10;
    public static final int  LOG_TAIL_LINES            = 20;

    // HTTP
    public static final int    HTTP_OK                = 200;
    public static final int    HTTP_CONNECT_TIMEOUT_S = 10;
    public static final String HTTP_INTERNAL_ERROR    = "internal_error";
    public static final String HTTP_INTERNAL_ERROR_UC = "INTERNAL_ERROR";

    // Archive
    public static final int    BUFFER_SIZE       = 8192;
    public static final String TEMP_FILE_PREFIX  = "zulu-fx-25";
    public static final String TEMP_FILE_SUFFIX  = ".zip";
    public static final String ARCHIVE_TYPE_ZIP  = "zip";
    public static final String JAVA_PACKAGE_TYPE = "jdk";

    // OS Detection
    public static final String OS_WINDOWS  = "windows";
    public static final String OS_MACOS    = "macos";
    public static final String OS_LINUX    = "linux";
    public static final String ARCH_ARM    = "arm";
    public static final String ARCH_X86    = "x86";
    public static final String ARCH_X86_64 = "x86_64";

    // JavaFX Validation
    public static final String JAVAFX_CONTROLS_MODULE = "javafx.controls";
    public static final String JDK_COMPILER_MODULE    = "jdk.compiler";

    // XML Tags
    public static final String XML_TAG_RELEASE = "<release>(.*?)</release>";
    public static final String XML_TAG_LATEST  = "<latest>(.*?)</latest>";

    // JVM Arguments
    public static final String JVM_ARG_SOURCE              = "--source";
    public static final String JVM_ARG_LIST_MODULES        = "--list-modules";
    public static final String JVM_ARG_MAC_FORK            = "-Djdk.lang.Process.launchMechanism=FORK";
    public static final String JVM_ARG_TRUSTSTORE          = "-Djavax.net.ssl.trustStore=";
    public static final String JVM_ARG_TRUSTSTORE_PASSWORD = "-Djavax.net.ssl.trustStorePassword=";
    public static final String SYSPROP_TRUSTSTORE          = "javax.net.ssl.trustStore";
    public static final String SYSPROP_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    // Launch Arguments
    public static final String ARG_JAR    = "--jar";
    public static final String ARG_GAV    = "--gav";
    public static final String ARG_CONFIG = "-Dosgifx.config=";

    // Log Format
    public static final String LOG_FILE_FORMAT = "osgifx-%s.log";
}
