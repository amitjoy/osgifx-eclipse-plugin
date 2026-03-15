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
package com.osgifx.eclipse.internal.launcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.osgifx.eclipse.internal.Activator;
import com.osgifx.eclipse.internal.storage.ConnectionProfile;

public final class OsgifxProcessLauncher extends Job {

    private static final Map<String, Long> activeProcesses = new ConcurrentHashMap<>();

    private final ConnectionProfile profile;
    private final Path              configPath;
    private final Path              javaExePath;
    private final Path              scriptPath;
    private final String            gav;
    private final String            localJar;

    public OsgifxProcessLauncher(final ConnectionProfile profile,
                                 final Path configPath,
                                 final Path javaExePath,
                                 final Path scriptPath,
                                 final String gav,
                                 final String localJar) {
        super("Launching OSGi.fx");
        this.profile     = profile;
        this.configPath  = configPath;
        this.javaExePath = javaExePath;
        this.scriptPath  = scriptPath;
        this.gav         = gav;
        this.localJar    = localJar;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        try {
            monitor.beginTask("Launching OSGi.fx", IProgressMonitor.UNKNOWN);

            final var cmd = buildCommand();
            final var pb  = new ProcessBuilder(cmd);

            pb.directory(scriptPath.getParent().toFile());
            pb.redirectErrorStream(true);

            // Detach from Eclipse - don't inherit IO
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            final var process = pb.start();

            // Store PID for cleanup
            activeProcesses.put(profile.id, process.pid());

            // Add shutdown hook for cleanup on Eclipse exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));

            return Status.OK_STATUS;
        } catch (final Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to launch OSGi.fx: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }

    private List<String> buildCommand() {
        final var cmd = new ArrayList<String>();
        cmd.add(javaExePath.toString());
        cmd.add("--source");
        cmd.add("25");
        cmd.add(scriptPath.toString());
        if (localJar != null && !localJar.isBlank()) {
            cmd.add("--jar");
            cmd.add(localJar);
        } else {
            cmd.add("--gav");
            cmd.add(gav);
        }
        cmd.add("-Dosgifx.config=" + configPath);
        return cmd;
    }

    public static Long getProcessId(final String profileId) {
        return activeProcesses.get(profileId);
    }

    public static void terminateProcess(final String profileId) {
        final var pid = activeProcesses.get(profileId);
        if (pid != null) {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
            activeProcesses.remove(profileId);
        }
    }
}
