/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
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
import static org.hamcrest.CoreMatchers.not;
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
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.LogVerifier;
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
        assertThat(repo, is(notNullValue()));
    }

    @Test
    public void testOfflineLoadingFromCache() throws Exception {
        RemoteAgent onlineAgent = newOnlineAgent();
        loadHttpRepository(onlineAgent);

        assertThat(localServer.getAccessedUrls(HTTP_REPO_PATH), not(is(Collections.<String> emptyList()))); // self-test
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

        assertThat(localServer.getAccessedUrls(HTTP_REPO_PATH), not(is(Collections.<String> emptyList()))); // self-test
        localServer.clearAccessedUrls(HTTP_REPO_PATH);

        IMetadataRepository repo = loadHttpRepository(onlineAgent);
        assertThat(repo, is(notNullValue()));

        assertThat(localServer.getAccessedUrls(HTTP_REPO_PATH), is(Collections.<String> emptyList()));
    }

    private RemoteAgent newOnlineAgent() throws Exception {
        return new RemoteAgent(new MavenContextImpl(localMavenRepository, false, logVerifier.getLogger(),
                new Properties()));
    }

    private RemoteAgent newOfflineAgent() throws Exception {
        return new RemoteAgent(new MavenContextImpl(localMavenRepository, true, logVerifier.getLogger(),
                new Properties()));
    }

    private IMetadataRepository loadHttpRepository(RemoteAgent agent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }

}
