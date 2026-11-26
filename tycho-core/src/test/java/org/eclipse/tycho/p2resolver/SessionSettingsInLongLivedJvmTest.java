/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Contributors to the Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Singletons survive the session in a JVM that runs several builds (e.g. Maven Daemon). They must
 * use the settings of the current build, not those of the build in which they were created.
 */
@ExtendWith(TychoPlexusExtension.class)
public class SessionSettingsInLongLivedJvmTest {

    private static final URI REPOSITORY_URL = URI.create("https://repo.example.org/p2");

    @Inject
    private PlexusContainer container;

    @Test
    public void testCredentialsOfNextBuild() throws Exception {
        MavenRepositorySettings settings = container.lookup(MavenRepositorySettings.class);
        MavenRepositoryLocation location = new MavenRepositoryLocation("server", REPOSITORY_URL);
        assertNull(settings.getCredentials(location));

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Server server = new Server();
        server.setId("server");
        server.setUsername("user");
        server.setPassword("password");
        request.addServer(server);
        startNextBuild(request, new DefaultRepositorySystemSession());

        Credentials credentials = settings.getCredentials(location);
        assertNotNull(credentials);
        assertEquals("user", credentials.getUserName());
    }

    @Test
    public void testProxyOfNextBuildOnThreadOfPreviousBuild() throws Exception {
        ProxyHelper proxyHelper = container.lookup(ProxyHelper.class);
        assertEquals(Proxy.NO_PROXY, proxyHelper.getProxy(REPOSITORY_URL));
        // like the threads of an HTTP client, this thread is not managed by Maven and outlives the build
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
            }).get();

            DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
            repositorySession.setProxySelector(new DefaultProxySelector()
                    .add(new org.eclipse.aether.repository.Proxy("http", "proxy.example.org", 8080), null));
            startNextBuild(new DefaultMavenExecutionRequest(), repositorySession);

            Proxy proxy = executor.submit(() -> proxyHelper.getProxy(REPOSITORY_URL)).get();
            assertEquals(Proxy.Type.HTTP, proxy.type());
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            assertEquals("proxy.example.org", address.getHostString());
            assertEquals(8080, address.getPort());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Ends the session of the current build and starts a new one, as Maven does for each build.
     */
    @SuppressWarnings("deprecation")
    private void startNextBuild(MavenExecutionRequest request, DefaultRepositorySystemSession repositorySession)
            throws Exception {
        MavenSession session = new MavenSession(container, repositorySession, request,
                new DefaultMavenExecutionResult());
        SessionScope sessionScope = container.lookup(SessionScope.class);
        sessionScope.exit();
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, session);
        container.lookup(LegacySupport.class).setSession(session);
    }
}
