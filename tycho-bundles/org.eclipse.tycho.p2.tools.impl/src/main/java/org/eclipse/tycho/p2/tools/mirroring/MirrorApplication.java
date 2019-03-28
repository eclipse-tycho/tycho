/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.Mirroring;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.resolver.shared.RepositoryReference;

@SuppressWarnings("restriction")
public class MirrorApplication extends org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication {

    private final boolean includePackedArtifacts;
    private final Map<String, String> extraArtifactRepositoryProperties;
    private final List<RepositoryReference> repositoryReferences;

    public MirrorApplication(IProvisioningAgent agent, boolean includePackedArtifacts,
            Map<String, String> extraArtifactRepositoryProperties, List<RepositoryReference> repositoryReferences) {
        super();
        this.agent = agent;
        this.includePackedArtifacts = includePackedArtifacts;
        this.extraArtifactRepositoryProperties = extraArtifactRepositoryProperties;
        this.repositoryReferences = repositoryReferences;
        this.removeAddedRepositories = false;
    }

    @Override
    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        IArtifactRepository result = super.initializeDestination(toInit, mgr);
        // simple.SimpleArtifactRepository.PUBLISH_PACK_FILES_AS_SIBLINGS is not public
        result.setProperty("publishPackFilesAsSiblings", "true");
        extraArtifactRepositoryProperties.entrySet()
                .forEach(entry -> result.setProperty(entry.getKey(), entry.getValue()));
        return result;
    }

    @Override
    protected IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr)
            throws ProvisionException {
        IMetadataRepository result = super.initializeDestination(toInit, mgr);
        List<? extends IRepositoryReference> iRepoRefs = repositoryReferences.stream()
                .map(this::toSpiRepositoryReference).collect(Collectors.toList());
        result.addReferences(iRepoRefs);
        return result;
    }

    private org.eclipse.equinox.p2.repository.spi.RepositoryReference toSpiRepositoryReference(RepositoryReference rr) {
        return new org.eclipse.equinox.p2.repository.spi.RepositoryReference(URI.create(rr.getLocation()), rr.getName(),
                IRepository.TYPE_METADATA, rr.isEnable() ? IRepository.ENABLED : IRepository.NONE);
    }

    @Override
    protected Mirroring getMirroring(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor) {
        Mirroring mirroring = super.getMirroring(slice, monitor);
        mirroring.setIncludePacked(includePackedArtifacts);
        return mirroring;
    }
}
