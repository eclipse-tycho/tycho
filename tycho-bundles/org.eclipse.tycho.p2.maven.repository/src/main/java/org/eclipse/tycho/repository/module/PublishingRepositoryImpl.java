/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
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

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;

public class PublishingRepositoryImpl implements PublishingRepository {
    private final ReactorProjectIdentities project;

    private final ModuleMetadataRepository metadataRepository;
    private final ModuleArtifactRepository artifactRepository;

    public PublishingRepositoryImpl(IProvisioningAgent agent, ReactorProjectIdentities project) {
        this.project = project;

        PublishingRepositoryLoader loadHelper = new PublishingRepositoryLoader(agent, project);
        this.metadataRepository = loadHelper.getModuleMetadataRepository();
        this.artifactRepository = loadHelper.getModuleArtifactRepository();
    }

    @Override
    public ReactorProjectIdentities getProjectIdentities() {
        return project;
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

    @Override
    public Set<Object> getInstallableUnits() {
        Set<Object> result = new HashSet<>();
        result.addAll(getMetadataRepository().query(QueryUtil.ALL_UNITS, null).toSet());
        return result;
    }

    @Override
    public IRawArtifactFileProvider getArtifacts() {
        return artifactRepository;
    }

    @Override
    public IFileArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    @Override
    public IArtifactRepository getArtifactRepositoryForWriting(WriteSessionContext writeSession) {
        return new ModuleArtifactRepositoryDelegate(artifactRepository, writeSession);
    }

    @Override
    public void addArtifactLocation(String classifier, File artifactLocation) throws ProvisionException {
        artifactRepository.getArtifactsMap().add(classifier, artifactLocation);
    }

    @Override
    public Map<String, File> getArtifactLocations() {
        Map<String, File> artifactLocations = artifactRepository.getArtifactsMap().getLocalArtifactLocations();

        // add storage files of the repositories themselves
        artifactLocations.put(TychoConstants.CLASSIFIER_P2_METADATA, metadataRepository.getPersistenceFile());
        // TODO the artifacts.xml entry is already in the map - maybe it should only be added here

        return artifactLocations;
    }
}
