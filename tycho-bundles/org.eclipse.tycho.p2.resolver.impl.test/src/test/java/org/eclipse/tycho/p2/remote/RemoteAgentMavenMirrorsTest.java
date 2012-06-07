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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.MemoryLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteAgentMavenMirrorsTest {

    private static final boolean OFFLINE = false;

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private MavenLogger logger = new MemoryLog(true);

    private HttpServer localServer;

    private MavenRepositorySettingsStub mavenRepositorySettings;
    private IProvisioningAgent subject;

    @Before
    public void initServer() throws Exception {
        localServer = HttpServer.startServer();
    }

    @Before
    public void initSubject() throws ProvisionException {
        File localRepository = tempManager.newFolder("localRepo");
        MavenContext mavenContext = new MavenContextImpl(localRepository, OFFLINE, logger);

        mavenRepositorySettings = new MavenRepositorySettingsStub();
        subject = new RemoteAgent(mavenContext, mavenRepositorySettings, OFFLINE);
    }

    @Test
    public void testLoadFromOriginalLocation() throws Exception {
        String repositoryId = "other-id";
        URI url = URI.create(localServer.addServer("original", ResourceUtil.resourceFile("repositories/e342")));

        Repositories repos = loadRepositories(repositoryId, url);

        assertThat(repos.getMetadataRepository(), notNullValue());
        assertThat(repos.getArtifactRepository(), notNullValue());
    }

    @Test
    public void testLoadFromMirroredLocation() throws Exception {
        String repositoryId = "well-known-id";
        URI originalUrl = URI.create(localServer.addServer("original", noContent())); // will fail if used
        URI mirroredUrl = URI.create(localServer.addServer("mirrored", ResourceUtil.resourceFile("repositories/e342")));
        prepareMavenMirrorConfiguration(repositoryId, mirroredUrl);

        Repositories repos = loadRepositories(repositoryId, originalUrl);

        assertThat(repos.getMetadataRepository(), notNullValue());
        assertThat(repos.getArtifactRepository(), notNullValue());
    }

    @Test
    public void testLoadFromMirroredLocationWithFallbackId() throws Exception {
        URI originalUrl = URI.create(localServer.addServer("original", noContent())); // will fail if used
        URI mirroredUrl = URI.create(localServer.addServer("mirrored", ResourceUtil.resourceFile("repositories/e342")));
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

    private File noContent() {
        return tempManager.newFolder("empty");
    }

    private Repositories loadRepositories(String id, URI specifiedUrl) throws Exception {

        IRepositoryIdManager idManager = (IRepositoryIdManager) subject.getService(IRepositoryIdManager.SERVICE_NAME);
        idManager.addMapping(id, specifiedUrl);

        IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) subject
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository metadataRepo = metadataManager.loadRepository(specifiedUrl, null);

        IArtifactRepositoryManager artifactsManager = (IArtifactRepositoryManager) subject
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        IArtifactRepository artifactsRepo = artifactsManager.loadRepository(specifiedUrl, null);

        return new Repositories(metadataRepo, artifactsRepo);
    }

    static class MavenRepositorySettingsStub implements MavenRepositorySettings {
        private Map<String, URI> idToMirrorMap = new HashMap<String, URI>();

        public void addMirror(String repositoryId, URI mirroredUrl) {
            idToMirrorMap.put(repositoryId, mirroredUrl);
        }

        public MavenRepositoryLocation getMirror(MavenRepositoryLocation repository) {
            if (idToMirrorMap.containsKey(repository.getId())) {
                return new MavenRepositoryLocation("mirror-id", idToMirrorMap.get(repository.getId()));
            }
            return null;
        }

        public Credentials getCredentials(MavenRepositoryLocation location) {
            // TODO Auto-generated method stub
            return null;
        }
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
