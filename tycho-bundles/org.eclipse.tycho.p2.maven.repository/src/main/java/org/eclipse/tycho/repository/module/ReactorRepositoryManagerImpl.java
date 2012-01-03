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
package org.eclipse.tycho.repository.module;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.BuildOutputDirectory;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    /**
     * This value is ignored anyway. See for example
     * {@link ModuleMetadataRepositoryFactory#create(URI, String, String, Map)}
     */
    private static final String BUILD_REPOSITORY_NAME = "";

    private IProvisioningAgentProvider agentFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    public void activateManager() throws IOException, ProvisionException {
        agentDir = createTempDir("tycho_reactor_agent");
        agent = agentFactory.createAgent(agentDir.toURI());
    }

    public void deactivateManager() {
        agent.stop();
        FileUtils.deleteAll(agentDir);
    }

    // TODO hide?
    public IProvisioningAgent getAgent() {
        return agent;
    }

    public PublishingRepository getPublishingRepository(BuildOutputDirectory buildDirectory) {
        return new PublishingRepositoryView(getBuildMetadataRepository(buildDirectory),
                getBuildArtifactRepository(buildDirectory));
    }

    public PublishingRepository getPublishingRepositoryForWriting(BuildOutputDirectory buildDirectory,
            WriteSessionContext writeSession) {
        return new PublishingRepositoryViewForWriting(getBuildMetadataRepository(buildDirectory),
                getBuildArtifactRepository(buildDirectory), writeSession);
    }

    private ModuleMetadataRepository getBuildMetadataRepository(BuildOutputDirectory buildDirectory) {
        // TODO use p2metadata.xml file instead of folder? Could prevent loading a content.xml from the folder
        final URI location = buildDirectory.getLocation().toURI();

        IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        try {
            return (ModuleMetadataRepository) repoManager.loadRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException e) {
            if (e.getStatus() != null && e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
                // repository doesn't exist yet; create it 
                return createBuildMetadataRepository(location, repoManager);

            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private ModuleMetadataRepository createBuildMetadataRepository(final URI location,
            IMetadataRepositoryManager repoManager) {
        try {
            return (ModuleMetadataRepository) repoManager.createRepository(location, BUILD_REPOSITORY_NAME,
                    ModuleMetadataRepository.REPOSITORY_TYPE, EMPTY_MAP);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    private ModuleArtifactRepository getBuildArtifactRepository(BuildOutputDirectory buildDirectory) {
        // TODO use p2artifacts.xml file instead of folder? Could prevent loading a artifacts.xml from the folder
        final URI location = buildDirectory.getLocation().toURI();

        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        try {
            return (ModuleArtifactRepository) repoManager.loadRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException e) {
            if (e.getStatus() != null && e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
                // repository doesn't exist yet; create it 
                return createBuildArtifactRepository(location, repoManager, buildDirectory);

            } else {
                // TODO dedicated message?
                throw new RuntimeException(e);
            }
        }
    }

    private ModuleArtifactRepository createBuildArtifactRepository(URI location,
            IArtifactRepositoryManager repoManager, BuildOutputDirectory buildDirectory) {
        try {
            return (ModuleArtifactRepository) repoManager.createRepository(location, BUILD_REPOSITORY_NAME,
                    ModuleArtifactRepository.REPOSITORY_TYPE, EMPTY_MAP);
        } catch (ProvisionException e) {
            // TODO dedicated message?
            throw new RuntimeException(e);
        }
    }

    private static File createTempDir(String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "");
        tempFile.delete();
        tempFile.mkdirs();
        if (!tempFile.isDirectory()) {
            throw new IOException("Failed to create temporary directory: " + tempFile);
        }
        return tempFile;
    }
}
