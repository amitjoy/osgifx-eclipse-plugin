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
package com.osgifx.eclipse.internal.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.osgifx.eclipse.internal.Activator;

/**
 * Downloads the {@code com.osgifx.console.agent} JAR from Maven Central.
 *
 * <p>
 * The agent is NOT used by this Eclipse plugin directly. It must be manually
 * deployed by the user into their target OSGi runtime. This utility simply
 * resolves the latest available version and downloads the JAR to a
 * user-specified location.
 * </p>
 *
 * <p>
 * Uses {@code java.net.http.HttpClient} with an automatic HTTP/2 → HTTP/1.1
 * fallback to handle environments where ALPN negotiation fails (e.g. corporate
 * SSL-intercepting proxies).
 * </p>
 */
public final class AgentDownloader {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final String GROUP_ID      = "com.osgifx";
    private static final String ARTIFACT_ID   = "com.osgifx.console.agent";

    /**
     * Resolves the latest release version of the agent from Maven Central metadata.
     *
     * @return the latest release version string, e.g. {@code "3.0.0"}
     * @throws IOException if a network or I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public String resolveLatestVersion() throws IOException, InterruptedException {
        final var metadataUrl = MAVEN_CENTRAL + GROUP_ID.replace('.', '/') + "/" + ARTIFACT_ID + "/maven-metadata.xml";
        Activator.log(IStatus.INFO, "Resolving latest agent version from: " + metadataUrl, null);

        final var xml     = fetchStringWithFallback(URI.create(metadataUrl));
        Matcher   matcher = Pattern.compile("<release>(.*?)</release>").matcher(xml);
        if (matcher.find()) {
            final var version = matcher.group(1);
            Activator.log(IStatus.INFO, "Latest agent version resolved: " + version, null);
            return version;
        }
        matcher = Pattern.compile("<latest>(.*?)</latest>").matcher(xml);
        if (matcher.find()) {
            final var version = matcher.group(1);
            Activator.log(IStatus.INFO, "Latest agent version resolved: " + version, null);
            return version;
        }
        throw new IOException("Could not resolve agent version from maven-metadata.xml at: " + metadataUrl);
    }

    /**
     * Downloads the agent JAR to the specified target directory.
     *
     * @param targetDirectory the directory into which the JAR will be saved
     * @param monitor progress monitor; may be {@code null}
     * @return the {@link Path} of the saved JAR file
     * @throws IOException if a network or I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public Path download(final Path targetDirectory,
                         final IProgressMonitor monitor) throws IOException, InterruptedException {
        final var version   = resolveLatestVersion();
        final var jarName   = ARTIFACT_ID + "-" + version + ".jar";
        final var groupPath = GROUP_ID.replace('.', '/');
        final var uri       = URI.create(MAVEN_CENTRAL + groupPath + "/" + ARTIFACT_ID + "/" + version + "/" + jarName);

        Activator.log(IStatus.INFO, "Downloading agent JAR from: " + uri, null);

        if (monitor != null) {
            monitor.subTask("Downloading " + jarName + "...");
        }

        try (final InputStream body = fetchStreamWithFallback(uri)) {
            Files.createDirectories(targetDirectory);
            final var targetFile = targetDirectory.resolve(jarName);
            Files.copy(body, targetFile, StandardCopyOption.REPLACE_EXISTING);
            Activator.log(IStatus.INFO, "Agent JAR downloaded to: " + targetFile.toAbsolutePath(), null);
            return targetFile;
        } catch (final IOException e) {
            Activator.log(IStatus.ERROR, "Failed to download agent JAR from: " + uri, e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // HTTP fallback helpers
    // -------------------------------------------------------------------------

    private String fetchStringWithFallback(final URI uri) throws IOException, InterruptedException {
        try {
            return sendStringRequest(uri, HttpClient.Version.HTTP_2);
        } catch (final IOException e) {
            if (isAlpnInternalError(e)) {
                Activator.log(IStatus.WARNING,
                        "HTTP/2 request to " + uri + " failed (" + e.getMessage() + "), retrying with HTTP/1.1...",
                        null);
                return sendStringRequest(uri, HttpClient.Version.HTTP_1_1);
            }
            throw e;
        }
    }

    private InputStream fetchStreamWithFallback(final URI uri) throws IOException, InterruptedException {
        try {
            return sendStreamRequest(uri, HttpClient.Version.HTTP_2);
        } catch (final IOException e) {
            if (isAlpnInternalError(e)) {
                Activator.log(IStatus.WARNING,
                        "HTTP/2 request to " + uri + " failed (" + e.getMessage() + "), retrying with HTTP/1.1...",
                        null);
                return sendStreamRequest(uri, HttpClient.Version.HTTP_1_1);
            }
            throw e;
        }
    }

    private String sendStringRequest(final URI uri,
                                     final HttpClient.Version version) throws IOException, InterruptedException {
        final var client   = buildClient(version);
        final var request  = HttpRequest.newBuilder(uri).GET().build();
        final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP " + response.statusCode() + " from " + uri);
        }
        return response.body();
    }

    private InputStream sendStreamRequest(final URI uri,
                                          final HttpClient.Version version) throws IOException, InterruptedException {
        final var client   = buildClient(version);
        final var request  = HttpRequest.newBuilder(uri).GET().build();
        final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP " + response.statusCode() + " from " + uri);
        }
        return response.body();
    }

    private HttpClient buildClient(final HttpClient.Version version) {
        return HttpClient.newBuilder().version(version).followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Detects the ALPN negotiation failure fingerprint — a chain of exceptions
     * containing the text {@code "internal_error"} — which occurs when an
     * SSL-intercepting proxy rejects HTTP/2 ALPN negotiation.
     */
    private boolean isAlpnInternalError(final IOException e) {
        Throwable cause = e;
        while (cause != null) {
            final var msg = cause.getMessage();
            if (msg != null && (msg.contains("internal_error") || msg.contains("INTERNAL_ERROR"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
