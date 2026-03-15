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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public final class ConnectionProfileStore {

    private static final Type LIST_TYPE = TypeToken.getParameterized(List.class, ConnectionProfile.class).getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File storeFile;

    public ConnectionProfileStore(final File storeFile) {
        this.storeFile = storeFile;
    }

    public List<ConnectionProfile> loadAll() {
        if (!storeFile.exists()) {
            return new ArrayList<>();
        }
        try (final var reader = new FileReader(storeFile)) {
            final var profiles = gson.<List<ConnectionProfile>> fromJson(reader, LIST_TYPE);
            return profiles != null ? profiles : new ArrayList<>();
        } catch (final IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveAll(final List<ConnectionProfile> profiles) {
        storeFile.getParentFile().mkdirs();
        try (final var writer = new FileWriter(storeFile)) {
            gson.toJson(profiles, writer);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save connection profiles", e);
        }
    }

    public void add(final ConnectionProfile profile) {
        final var profiles = new ArrayList<>(loadAll());
        profiles.add(profile);
        saveAll(profiles);
    }

    public void update(final ConnectionProfile profile) {
        final var profiles = new ArrayList<>(loadAll());
        final var index    = findIndex(profiles, profile.id);
        if (index >= 0) {
            profiles.set(index, profile);
            saveAll(profiles);
        }
    }

    public void remove(final String id) {
        final var profiles = new ArrayList<>(loadAll());
        profiles.removeIf(p -> p.id.equals(id));
        saveAll(profiles);
    }

    public void duplicate(final String id) {
        final var profiles = new ArrayList<>(loadAll());
        find(profiles, id).ifPresent(original -> {
            final var copy = gson.fromJson(gson.toJson(original), ConnectionProfile.class);
            copy.id            = UUID.randomUUID().toString();
            copy.name          = original.name + " (Copy)";
            copy.lastConnected = null;
            copy.lastStatus    = null;
            profiles.add(copy);
            saveAll(profiles);
        });
    }

    public Optional<ConnectionProfile> find(final List<ConnectionProfile> profiles, final String id) {
        return profiles.stream().filter(p -> p.id.equals(id)).findFirst();
    }

    private int findIndex(final List<ConnectionProfile> profiles, final String id) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
