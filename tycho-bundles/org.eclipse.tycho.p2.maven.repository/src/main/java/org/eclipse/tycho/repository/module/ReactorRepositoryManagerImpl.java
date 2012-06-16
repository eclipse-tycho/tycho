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
import org.eclipse.tycho.ReactorProjectId;
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

    public PublishingRepository getPublishingRepository(ReactorProjectId project) {
        return new PublishingRepositoryView(getBuildMetadataRepository(project), getBuildArtifactRepository(project));
    }

    public PublishingRepository getPublishingRepositoryForArtifactWriting(ReactorProjectId project,
            WriteSessionContext writeSession) {
        return new PublishingRepositoryViewForWriting(getBuildMetadataRepository(project),
                getBuildArtifactRepository(project), writeSession);
    }

    private ModuleMetadataRepository getBuildMetadataRepository(ReactorProjectId project) {
        // TODO use p2metadata.xml file instead of folder? Could prevent loading a content.xml from the folder
        final URI location = project.getBuildDirectory().getLocation().toURI();

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
            // TODO pass GAV as properties
            return (ModuleMetadataRepository) repoManager.createRepository(location, BUILD_REPOSITORY_NAME,
                    ModuleMetadataRepository.REPOSITORY_TYPE, EMPTY_MAP);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    private ModuleArtifactRepository getBuildArtifactRepository(ReactorProjectId project) {
        // TODO use p2artifacts.xml file instead of folder? Could prevent loading a artifacts.xml from the folder
        final URI location = project.getBuildDirectory().getLocation().toURI();

        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        try {
            return (ModuleArtifactRepository) repoManager.loadRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException e) {
            if (e.getStatus() != null && e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
                // repository doesn't exist yet; create it 
                return createBuildArtifactRepository(location, repoManager, project);

            } else {
                // TODO dedicated message?
                throw new RuntimeException(e);
            }
        }
    }

    private ModuleArtifactRepository createBuildArtifactRepository(URI location,
            IArtifactRepositoryManager repoManager, ReactorProjectId project) {
        try {
            // TODO pass GAV as properties
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
