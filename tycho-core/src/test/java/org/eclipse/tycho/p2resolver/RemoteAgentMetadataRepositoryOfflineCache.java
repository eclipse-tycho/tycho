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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.apache.maven.execution.MavenSession;
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
public class RemoteAgentMetadataRepositoryOfflineCache implements TychoPlexusExtension.MavenSessionCustomizer {

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
    public void initLocalMavenRepository() throws Exception {
        localHttpRepo = URI
                .create(localServer.addServlet(HTTP_REPO_PATH, new File("src/test/resources/repositories/e342")));
        newFolder("m2-repo");
    }

    @Override
    public void customizeSession(MavenSession mavenSession) {
        mavenSession.getRequest().setOffline(true);
    }

    @Test
    public void testOfflineLoadingWithoutCacheFails() throws Exception {
        IProvisioningAgent offlineAgent = newOfflineAgent();
        assertThrows(ProvisionException.class, () -> loadHttpRepository(offlineAgent));
    }

    private IProvisioningAgent newOfflineAgent() throws Exception {
        return container.lookup(IProvisioningAgent.class);
    }

    private IMetadataRepository loadHttpRepository(IProvisioningAgent offlineAgent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = offlineAgent
                .getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempManager.resolve(path)).toFile();
    }
}
