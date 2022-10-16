/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.testutil.HttpServer;
import org.eclipse.tycho.testutil.LogVerifier;
import org.eclipse.tycho.testutil.MockMavenContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for verifying the caching behaviour of the {@link RemoteAgent}'s metadata repository
 * manager.
 */
public class RemoteAgentMetadataRepositoryCacheTest {

    private static final String HTTP_REPO_PATH = "e342";

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final HttpServer localServer = new HttpServer();
    private URI localHttpRepo;

    private File localMavenRepository;

    @Before
    public void startHttpServer() throws Exception {
        localHttpRepo = URI.create(localServer.addServlet(HTTP_REPO_PATH, new File("resources/repositories/e342")));
    }

    @Before
    public void initLocalMavenRepository() throws Exception {
        localMavenRepository = tempManager.newFolder("m2-repo");
    }

    @Test
    public void testOnlineLoading() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertNotNull(repo);
    }

    @Test
    public void testOfflineLoadingFromCache() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        assertFalse(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty()); // self-test
        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        RemoteAgent offlineAgent = newOfflineAgent();
        IMetadataRepository repo = loadHttpRepository(offlineAgent);
        assertNotNull(repo);

        assertTrue(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty());
    }

    @Test(expected = ProvisionException.class)
    public void testOfflineLoadingWithoutCacheFails() throws Exception {
        RemoteAgent offlineAgent = newOfflineAgent();
        loadHttpRepository(offlineAgent);
    }

    @Test
    public void testOnlineLoadingFallsBackToCache() throws Exception {
        RemoteAgent onlineAgent1 = newOnlineAgent();
        loadHttpRepository(onlineAgent1);

        // server becomes unavailable
        localServer.stop();

        RemoteAgent onlineAgent2 = newOnlineAgent();
        IMetadataRepository repo = loadHttpRepository(onlineAgent2);
        assertNotNull(repo);
    }

    @Test(expected = ProvisionException.class)
    public void testOnlineLoadingFailsFastIfNoSourceAvailable() throws Exception {
        // server unavailable and no cache entry
        localServer.stop();

        RemoteAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);
    }

    @Test
    public void testOnlineReloadingDoesntReloadFromRemote() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        assertFalse(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty()); // self-test
        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertNotNull(repo);

        assertTrue(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty());
    }

    private RemoteAgent newOnlineAgent() throws Exception {
        return new RemoteAgent(
                new MockMavenContext(localMavenRepository, false, logVerifier.getLogger(), new Properties()));
    }

    private RemoteAgent newOfflineAgent() throws Exception {
        return new RemoteAgent(
                new MockMavenContext(localMavenRepository, true, logVerifier.getLogger(), new Properties()));
    }

    private IMetadataRepository loadHttpRepository(RemoteAgent agent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }

}
