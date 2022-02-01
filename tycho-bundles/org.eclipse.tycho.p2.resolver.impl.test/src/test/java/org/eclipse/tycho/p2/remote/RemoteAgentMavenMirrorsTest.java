/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.remote.testutil.MavenRepositorySettingsStub;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteAgentMavenMirrorsTest {

    private static final boolean OFFLINE = false;

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();
    @Rule
    public HttpServer localServer = new HttpServer();

    private MavenRepositorySettingsStub mavenRepositorySettings;
    private IProvisioningAgent subject;

    @Before
    public void initSubject() throws Exception {
        File localRepository = tempManager.newFolder("localRepo");
        MavenContext mavenContext = new MockMavenContext(localRepository, OFFLINE, logVerifier.getLogger(),
                new Properties());

        mavenRepositorySettings = new MavenRepositorySettingsStub();
        subject = new RemoteAgent(mavenContext, mavenRepositorySettings, OFFLINE);
    }

    @Test
    public void testLoadFromOriginalLocation() throws Exception {
        String repositoryId = "other-id";
        URI url = URI.create(localServer.addServlet("original", ResourceUtil.resourceFile("repositories/e342")));

        Repositories repos = loadRepositories(repositoryId, url);

        assertThat(repos.getMetadataRepository(), notNullValue());
        assertThat(repos.getArtifactRepository(), notNullValue());
    }

    @Test
    public void testLoadFromMirroredLocation() throws Exception {
        String repositoryId = "well-known-id";
        URI originalUrl = URI.create(localServer.addServlet("original", noContent())); // will fail if used
        URI mirroredUrl = URI
                .create(localServer.addServlet("mirrored", ResourceUtil.resourceFile("repositories/e342")));
        prepareMavenMirrorConfiguration(repositoryId, mirroredUrl);

        Repositories repos = loadRepositories(repositoryId, originalUrl);

        assertThat(repos.getMetadataRepository(), notNullValue());
        assertThat(repos.getArtifactRepository(), notNullValue());
    }

    @Test
    public void testLoadFromMirroredLocationWithFallbackId() throws Exception {
        URI originalUrl = URI.create(localServer.addServlet("original", noContent())); // will fail if used
        URI mirroredUrl = URI
                .create(localServer.addServlet("mirrored", ResourceUtil.resourceFile("repositories/e342")));
        String repositoryFallbackId = originalUrl.toString();
        assertFalse("self-test: fallback ID shall be URL without trailing slash", repositoryFallbackId.endsWith("/"));
        prepareMavenMirrorConfiguration(repositoryFallbackId, mirroredUrl);

        Repositories repos = loadRepositories(null, originalUrl);

        assertThat(repos.getMetadataRepository(), notNullValue());
        assertThat(repos.getArtifactRepository(), notNullValue());
    }

    private void prepareMavenMirrorConfiguration(String id, URI mirrorUrl) {
        mavenRepositorySettings.addMirror(id, mirrorUrl);
    }

    private File noContent() throws Exception {
        return tempManager.newFolder("empty");
    }

    private Repositories loadRepositories(String id, URI specifiedUrl) throws Exception {

        IRepositoryIdManager idManager = subject.getService(IRepositoryIdManager.class);
        idManager.addMapping(id, specifiedUrl);

        IMetadataRepositoryManager metadataManager = subject.getService(IMetadataRepositoryManager.class);
        IMetadataRepository metadataRepo = metadataManager.loadRepository(specifiedUrl, null);

        IArtifactRepositoryManager artifactsManager = subject.getService(IArtifactRepositoryManager.class);
        IArtifactRepository artifactsRepo = artifactsManager.loadRepository(specifiedUrl, null);

        return new Repositories(metadataRepo, artifactsRepo);
    }

    static class Repositories {
        private final IMetadataRepository metadataRepository;
        private final IArtifactRepository artifactRepository;

        Repositories(IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
            this.metadataRepository = metadataRepository;
            this.artifactRepository = artifactRepository;
        }

        public IMetadataRepository getMetadataRepository() {
            return metadataRepository;
        }

        public IArtifactRepository getArtifactRepository() {
            return artifactRepository;
        }
    }
}
