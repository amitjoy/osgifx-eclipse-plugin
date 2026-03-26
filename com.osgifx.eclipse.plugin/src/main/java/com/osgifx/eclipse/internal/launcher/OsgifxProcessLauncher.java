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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import com.osgifx.eclipse.internal.util.OSUtils;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class OsgifxProcessLauncher extends Job {

    private static final Map<String, Long> activeProcesses = new ConcurrentHashMap<>();
    private static final Map<String, Path> activeLogs      = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            activeProcesses.values().forEach(pid -> ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy));
        }));
    }

    private final ConnectionProfile profile;
    private final Path              configPath;
    private final Path              javaExePath;
    private final Path              scriptPath;
    private final String            gav;
    private final String            localJar;

    private Path logFile;

    public OsgifxProcessLauncher(final ConnectionProfile profile,
                                 final Path configPath,
                                 final Path javaExePath,
                                 final Path scriptPath,
                                 final String gav,
                                 final String localJar) {
        super("Launching OSGi.fx: " + profile.name);
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
            monitor.beginTask("Launching OSGi.fx", 100);

            // 1. Check if already running
            if (activeProcesses.containsKey(profile.id)) {
                final var pid = activeProcesses.get(profile.id);
                if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                      "OSGi.fx is already running for this profile");
                }
                activeProcesses.remove(profile.id);
            }

            final var cmd = buildCommand();
            Activator.log(IStatus.INFO, "Launching OSGi.fx with command: " + String.join(" ", cmd), null);

            final var pb = new ProcessBuilder(cmd);

            pb.directory(scriptPath.getParent().toFile());
            pb.redirectErrorStream(true);

            // Create log file
            final var logDir  = OsgifxWorkspaceUtil.getLogsLocation();
            final var logName = String.format("osgifx-%s.log", profile.id);
            logFile = new File(logDir, logName).toPath();
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));

            activeLogs.put(profile.id, logFile);
            Activator.log(IStatus.INFO, "OSGi.fx process output will be redirected to: " + logFile, null);

            monitor.worked(20);
            monitor.subTask("Starting process...");

            final var process = pb.start();
            final var pid     = process.pid();

            activeProcesses.put(profile.id, pid);
            Activator.log(IStatus.INFO, "OSGi.fx process started with PID: " + pid, null);

            monitor.worked(30);
            monitor.subTask("Verifying startup...");

            // Wait a bit to see if it crashes immediately
            for (var i = 0; i < 10; i++) {
                if (monitor.isCanceled()) {
                    process.destroy();
                    return Status.CANCEL_STATUS;
                }
                Thread.sleep(500);
                if (!process.isAlive()) {
                    final var exitCode = process.exitValue();
                    final var logTail  = readTailOfLog(logFile, 20);
                    final var message  = "OSGi.fx process (PID: " + pid + ") terminated unexpectedly with exit code "
                            + exitCode + ". Log file: " + logFile + "\n\n--- Last log output ---\n" + logTail;
                    Activator.log(IStatus.ERROR, message, null);
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
                }
                monitor.worked(5);
            }

            Activator.log(IStatus.INFO, "OSGi.fx process (PID: " + pid + ") is running normally.", null);
            return Status.OK_STATUS;
        } catch (final Exception e) {
            Activator.log(IStatus.ERROR, "Failed to launch OSGi.fx: " + e.getMessage(), e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to launch OSGi.fx: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }

    private String readTailOfLog(final Path logFile, final int lines) {
        try {
            final var all  = Files.readAllLines(logFile);
            final int from = Math.max(0, all.size() - lines);
            return String.join(System.lineSeparator(), all.subList(from, all.size()));
        } catch (final IOException e) {
            return "(could not read log: " + e.getMessage() + ")";
        }
    }

    private List<String> buildCommand() {
        final var cmd = new ArrayList<String>();
        cmd.add(javaExePath.toString());
        if (OSUtils.IS_OS_MAC) {
            cmd.add("-Djdk.lang.Process.launchMechanism=FORK");
        }

        final var trustStore = System.getProperty("javax.net.ssl.trustStore");
        if (trustStore != null) {
            cmd.add("-Djavax.net.ssl.trustStore=" + trustStore);
        }
        final var trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        if (trustStorePassword != null) {
            cmd.add("-Djavax.net.ssl.trustStorePassword=" + trustStorePassword);
        }

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

    public static Path getLogFile(final String profileId) {
        return activeLogs.get(profileId);
    }

    public static void terminateProcess(final String profileId) {
        final var pid = activeProcesses.get(profileId);
        if (pid != null) {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
            activeProcesses.remove(profileId);
        }
    }
}
