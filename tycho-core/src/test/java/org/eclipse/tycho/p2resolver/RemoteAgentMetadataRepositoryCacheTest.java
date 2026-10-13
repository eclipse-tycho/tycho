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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for verifying the caching behavior of the RemoteAgent's metadata repository manager.
 */
@ExtendWith(TychoPlexusExtension.class)
public class RemoteAgentMetadataRepositoryCacheTest {

    @Inject
    protected PlexusContainer container;

    private static final String HTTP_REPO_PATH = "e342";

    @TempDir
    Path tempManager;
    @RegisterExtension
    public LogVerifier logVerifier = new LogVerifier();

    @RegisterExtension
    public final HttpServer localServer = new HttpServer();
    private URI localHttpRepo;

    @BeforeEach
    public void startHttpServer() throws Exception {
        localHttpRepo = URI
                .create(localServer.addServlet(HTTP_REPO_PATH, new File("src/test/resources/repositories/e342")));
    }

    @BeforeEach
    public void initLocalMavenRepository() throws Exception {
        newFolder("m2-repo");
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

    @Test
    public void testOnlineLoadingFailsFastIfNoSourceAvailable() throws Exception {
        // server unavailable and no cache entry
        localServer.stop();

        IProvisioningAgent onlineAgent = newOnlineAgent();
        assertThrows(ProvisionException.class, () -> loadHttpRepository(onlineAgent));
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
        return container.lookup(IProvisioningAgent.class);
    }

    private IProvisioningAgent newOfflineAgent() throws Exception {
        return container.lookup(IProvisioningAgent.class);
    }

    private IMetadataRepository loadHttpRepository(IProvisioningAgent onlineAgent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = onlineAgent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempManager.resolve(path)).toFile();
    }
}
