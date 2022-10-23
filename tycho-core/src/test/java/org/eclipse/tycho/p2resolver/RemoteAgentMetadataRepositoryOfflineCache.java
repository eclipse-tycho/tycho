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

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.agent.RemoteAgent;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for verifying the caching behaviour of the {@link RemoteAgent}'s metadata repository
 * manager.
 */
public class RemoteAgentMetadataRepositoryOfflineCache extends TychoPlexusTestCase {

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
    public void initLocalMavenRepository() throws Exception {
        localHttpRepo = URI
                .create(localServer.addServlet(HTTP_REPO_PATH, new File("src/test/resources/repositories/e342")));
        localMavenRepository = tempManager.newFolder("m2-repo");
    }

    @Override
    protected void modifySession(MavenSession mavenSession) {
        mavenSession.getRequest().setOffline(true);
    }

    @Test(expected = ProvisionException.class)
    public void testOfflineLoadingWithoutCacheFails() throws Exception {
        RemoteAgent offlineAgent = newOfflineAgent();
        loadHttpRepository(offlineAgent);
    }

    private RemoteAgent newOnlineAgent() throws Exception {
        return new RemoteAgent(
                new MockMavenContext(localMavenRepository, false, logVerifier.getMavenLogger(), new Properties()),
                lookup(IProvisioningAgent.class));
    }

    private RemoteAgent newOfflineAgent() throws Exception {
        return new RemoteAgent(
                new MockMavenContext(localMavenRepository, true, logVerifier.getMavenLogger(), new Properties()),
                lookup(IProvisioningAgent.class));
    }

    private IMetadataRepository loadHttpRepository(RemoteAgent agent) throws ProvisionException {
        IMetadataRepositoryManager metadataRepositoryManager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repo = metadataRepositoryManager.loadRepository(localHttpRepo, null);
        return repo;
    }

}
