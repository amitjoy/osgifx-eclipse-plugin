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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.runtime.IStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.osgifx.eclipse.internal.Activator;

public final class VolatileConfigWriter {

    private static final String CONFIG_FILENAME = "config.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;

    public VolatileConfigWriter(final File configDir) {
        this.configDir = configDir;
    }

    public Path writeHeadlessConfig(final ConnectionProfile profile) {
        configDir.mkdirs();
        final var configFile     = new File(configDir, CONFIG_FILENAME);
        final var headlessConfig = convertToHeadlessFormat(profile);

        try (final var writer = new FileWriter(configFile)) {
            gson.toJson(headlessConfig, writer);
        } catch (final IOException e) {
            Activator.log(IStatus.ERROR, "Failed to write headless config to: " + configFile.getAbsolutePath(), e);
            throw new RuntimeException("Failed to write headless config", e);
        }

        Activator.log(IStatus.INFO, "Headless config written to: " + configFile.getAbsolutePath(), null);
        return configFile.toPath();
    }

    private JsonObject convertToHeadlessFormat(final ConnectionProfile profile) {
        final var root = new JsonObject();
        root.addProperty("type", profile.type);

        if ("SOCKET".equals(profile.type)) {
            final var socket = new JsonObject();
            socket.addProperty("host", profile.host);
            socket.addProperty("port", profile.port);
            socket.addProperty("timeout", profile.timeout);
            if (profile.password != null && !profile.password.isEmpty()) {
                socket.addProperty("password", profile.password);
            }
            if (profile.trustStorePath != null && !profile.trustStorePath.isEmpty()) {
                socket.addProperty("trustStorePath", profile.trustStorePath);
            }
            if (profile.trustStorePassword != null && !profile.trustStorePassword.isEmpty()) {
                socket.addProperty("trustStorePassword", profile.trustStorePassword);
            }
            root.add("socket", socket);
        } else if ("MQTT".equals(profile.type)) {
            final var mqtt = new JsonObject();
            mqtt.addProperty("server", profile.server);
            mqtt.addProperty("port", profile.mqttPort);
            mqtt.addProperty("timeout", profile.mqttTimeout);
            mqtt.addProperty("clientId", profile.clientId);
            if (profile.username != null && !profile.username.isEmpty()) {
                mqtt.addProperty("username", profile.username);
            }
            if (profile.mqttPassword != null && !profile.mqttPassword.isEmpty()) {
                mqtt.addProperty("password", profile.mqttPassword);
            }
            mqtt.addProperty("pubTopic", profile.pubTopic);
            mqtt.addProperty("subTopic", profile.subTopic);
            mqtt.addProperty("lwtTopic", profile.lwtTopic);

            if (profile.tokenConfig != null) {
                final var tokenConfig = new JsonObject();
                tokenConfig.addProperty("authServerURL", profile.tokenConfig.authServerURL);
                tokenConfig.addProperty("clientId", profile.tokenConfig.clientId);
                tokenConfig.addProperty("clientSecret", profile.tokenConfig.clientSecret);
                tokenConfig.addProperty("audience", profile.tokenConfig.audience);
                tokenConfig.addProperty("scope", profile.tokenConfig.scope);
                mqtt.add("tokenConfig", tokenConfig);
            }

            root.add("mqtt", mqtt);
        }

        return root;
    }
}
