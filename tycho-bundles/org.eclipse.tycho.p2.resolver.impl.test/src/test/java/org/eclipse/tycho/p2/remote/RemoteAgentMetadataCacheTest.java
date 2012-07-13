/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.MemoryLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteAgentMetadataCacheTest {

    private static final String HTTP_REPO_PATH = "e342";

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private HttpServer localServer;
    private URI localHttpRepo;

    private File localMavenRepository;

    private MavenLogger logger = new MemoryLog(false);

    @Before
    public void startHttpServer() throws Exception {
        localServer = HttpServer.startServer();
        localHttpRepo = URI.create(localServer.addServer(HTTP_REPO_PATH, new File("resources/repositories/e342")));
    }

    @After
    public void stopHttpServer() throws Exception {
        localServer.stop();
    }

    @Before
    public void initLocalMavenRepository() {
        localMavenRepository = tempManager.newFolder("m2-repo");
    }

    @Test
    public void testOnlineLoading() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertThat(repo, is(notNullValue()));
    }

    @Test
    public void testOfflineLoadingFromCache() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        RemoteAgent offlineAgent = newOfflineAgent();
        IMetadataRepository repo = loadHttpRepository(offlineAgent);
        assertThat(repo, is(notNullValue()));

        assertThat(localServer.getAccessedUrls(HTTP_REPO_PATH), is(Collections.<String> emptyList()));
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
        assertThat(repo, is(notNullValue()));
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

        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertThat(repo, is(notNullValue()));

        assertThat(localServer.getAccessedUrls(HTTP_REPO_PATH), is(Collections.<String> emptyList()));
    }

    private RemoteAgent newOnlineAgent() throws Exception {
        return new RemoteAgent(new MavenContextImpl(localMavenRepository, false, logger, new Properties()));
    }

    private RemoteAgent newOfflineAgent() throws Exception {
        return new RemoteAgent(new MavenContextImpl(localMavenRepository, true, logger, new Properties()));
    }

    private IMetadataRepository loadHttpRepository(RemoteAgent agent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }

}
