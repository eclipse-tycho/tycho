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
package org.eclipse.tycho.p2resolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for verifying the caching behavior of the RemoteAgent's metadata repository manager.
 */
public class RemoteAgentMetadataRepositoryCacheTest extends TychoPlexusTestCase {

    private static final String HTTP_REPO_PATH = "e342";

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final HttpServer localServer = new HttpServer();
    private URI localHttpRepo;

    @Before
    public void startHttpServer() throws Exception {
        localHttpRepo = URI
                .create(localServer.addServlet(HTTP_REPO_PATH, new File("src/test/resources/repositories/e342")));
    }

    @Before
    public void initLocalMavenRepository() throws Exception {
        tempManager.newFolder("m2-repo");
    }

    @Test
    public void testOnlineLoading() throws Exception {
        IProvisioningAgent onlineAgent = newOnlineAgent();
        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertNotNull(repo);
    }

    @Test
    public void testOfflineLoadingFromCache() throws Exception {
        IProvisioningAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        assertFalse(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty()); // self-test
        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        IProvisioningAgent offlineAgent = newOfflineAgent();
        IMetadataRepository repo = loadHttpRepository(offlineAgent);
        assertNotNull(repo);

        assertTrue(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty());
    }

    @Test
    public void testOnlineLoadingFallsBackToCache() throws Exception {
        IProvisioningAgent onlineAgent1 = newOnlineAgent();
        loadHttpRepository(onlineAgent1);

        // server becomes unavailable
        localServer.stop();

        IProvisioningAgent onlineAgent2 = newOnlineAgent();
        IMetadataRepository repo = loadHttpRepository(onlineAgent2);
        assertNotNull(repo);
    }

    @Test(expected = ProvisionException.class)
    public void testOnlineLoadingFailsFastIfNoSourceAvailable() throws Exception {
        // server unavailable and no cache entry
        localServer.stop();

        IProvisioningAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);
    }

    @Test
    public void testOnlineReloadingDoesntReloadFromRemote() throws Exception {
        IProvisioningAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        assertFalse(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty()); // self-test
        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertNotNull(repo);

        assertTrue(localServer.getAccessedUrls(HTTP_REPO_PATH).isEmpty());
    }

    private IProvisioningAgent newOnlineAgent() throws Exception {
        return lookup(IProvisioningAgent.class);
    }

    private IProvisioningAgent newOfflineAgent() throws Exception {
        return lookup(IProvisioningAgent.class);
    }

    private IMetadataRepository loadHttpRepository(IProvisioningAgent onlineAgent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = onlineAgent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }

}
