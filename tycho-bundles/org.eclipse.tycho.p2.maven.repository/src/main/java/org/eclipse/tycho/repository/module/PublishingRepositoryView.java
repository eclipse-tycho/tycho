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
import java.util.Map;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

// TODO make package private; currently only the test requires the public
public class PublishingRepositoryView implements PublishingRepository {

    private final ModuleMetadataRepository metadataRepository;
    private final ModuleArtifactRepository artifactRepository;

    public PublishingRepositoryView(ModuleMetadataRepository metadataRepository,
            ModuleArtifactRepository artifactRepository) {
        this.metadataRepository = metadataRepository;
        this.artifactRepository = artifactRepository;
    }

    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    public void addArtifactLocation(String classifier, File artifactLocation) throws ProvisionException {
        artifactRepository.getArtifactsMap().add(classifier, artifactLocation);
    }

    public Map<String, File> getArtifactLocations() {
        Map<String, File> artifactLocations = artifactRepository.getArtifactsMap().getLocalArtifactLocations();

        // add storage files of the repositories themselves
        artifactLocations.put(RepositoryLayoutHelper.CLASSIFIER_P2_METADATA, metadataRepository.getPersistenceFile());
        // TODO the artifacts.xml entry is already in the map - maybe it should only be added here

        return artifactLocations;
    }
}
