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
package com.osgifx.eclipse.internal.storage;

import java.util.UUID;

public final class ConnectionProfile {

    public String id;
    public String name;
    public String type; // "SOCKET" or "MQTT"

    // SOCKET fields
    public String host;
    public int    port;
    public int    timeout;
    public String password;
    public String trustStorePath;
    public String trustStorePassword;

    // MQTT fields
    public String      server;
    public int         mqttPort;
    public int         mqttTimeout;
    public String      clientId;
    public String      username;
    public String      mqttPassword;
    public String      pubTopic;
    public String      subTopic;
    public String      lwtTopic;
    public TokenConfig tokenConfig;

    // Status tracking
    public String lastConnected;
    public String lastStatus;   // "SUCCESS", "FAILURE", "NEVER"

    public static class TokenConfig {
        public String authServerURL;
        public String clientId;
        public String clientSecret;
        public String audience;
        public String scope;
    }

    public ConnectionProfile() {
        this.id = UUID.randomUUID().toString();
    }

    public ConnectionProfile(final String name, final String type) {
        this();
        this.name = name;
        this.type = type;
    }
}
