/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.ReactorProjectIdentities;

class PublishingRepositoryLoader {

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    /**
     * This value is ignored anyway. See for example
     * {@link ModuleMetadataRepositoryFactory#create(URI, String, String, Map)}
     */
    private static final String BUILD_REPOSITORY_NAME = "";

    private final IProvisioningAgent agent;
    private final ReactorProjectIdentities project;

    public PublishingRepositoryLoader(IProvisioningAgent agent, ReactorProjectIdentities project) {
        this.agent = agent;
        this.project = project;
    }

    public ModuleMetadataRepository getModuleMetadataRepository() {

        IMetadataRepositoryManager repoManager = agent.getService(IMetadataRepositoryManager.class);

        // TODO use p2metadata.xml file instead of folder? Could prevent loading a content.xml from the folder
        URI location = project.getBuildDirectory().getLocation().toURI();
        return getModuleMetadataRepository(repoManager, location);
    }

    private ModuleMetadataRepository getModuleMetadataRepository(IMetadataRepositoryManager repoManager, URI location) {
        try {
            return (ModuleMetadataRepository) repoManager.loadRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException e) {
            if (e.getStatus() != null && e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
                // repository doesn't exist yet; create it
                return createModuleMetadataRepository(repoManager, location);

            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private ModuleMetadataRepository createModuleMetadataRepository(IMetadataRepositoryManager repoManager,
            URI location) {
        try {
            return (ModuleMetadataRepository) repoManager.createRepository(location, BUILD_REPOSITORY_NAME,
                    ModuleMetadataRepository.REPOSITORY_TYPE, EMPTY_MAP);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    public ModuleArtifactRepository getModuleArtifactRepository() {
        IArtifactRepositoryManager repoManager = agent.getService(IArtifactRepositoryManager.class);

        // TODO use p2artifacts.xml file instead of folder? Could prevent loading a artifacts.xml from the folder
        final URI location = project.getBuildDirectory().getLocation().toURI();
        ModuleArtifactRepository moduleArtifactRepository = getModuleArtifactRepository(repoManager, location);

        // TODO encode the GAV in the URI so that this is not necessary
        moduleArtifactRepository.setGAV(project.getGroupId(), project.getArtifactId(), project.getVersion());

        return moduleArtifactRepository;
    }

    private ModuleArtifactRepository getModuleArtifactRepository(IArtifactRepositoryManager repoManager,
            final URI location) {
        try {
            return (ModuleArtifactRepository) repoManager.loadRepository(location,
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException e) {
            if (e.getStatus() != null && e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
                // repository doesn't exist yet; create it
                return createModuleArtifactRepository(repoManager, location);

            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private ModuleArtifactRepository createModuleArtifactRepository(IArtifactRepositoryManager repoManager,
            URI location) {
        try {
            return (ModuleArtifactRepository) repoManager.createRepository(location, BUILD_REPOSITORY_NAME,
                    ModuleArtifactRepository.REPOSITORY_TYPE, EMPTY_MAP);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

}
