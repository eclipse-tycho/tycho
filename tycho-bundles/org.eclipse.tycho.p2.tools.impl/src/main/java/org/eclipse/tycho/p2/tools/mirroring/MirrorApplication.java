/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

import bug377358.org.eclipse.equinox.p2.internal.repository.mirroring.Mirroring;


/**
 * {@link bug377358.org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication} that uses a
 * custom {@link IProvisioningAgent}.
 */
@SuppressWarnings("restriction")
public class MirrorApplication extends bug377358.org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication {

    private final boolean includePackedArtifacts;

    public MirrorApplication(IProvisioningAgent agent, boolean includePackedArtifacts) {
        super();
        this.agent = agent;
        this.includePackedArtifacts = includePackedArtifacts;
        this.removeAddedRepositories = false;
    }

    @Override
    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        IArtifactRepository result = super.initializeDestination(toInit, mgr);
        // simple.SimpleArtifactRepository.PUBLISH_PACK_FILES_AS_SIBLINGS is not public
        result.setProperty("publishPackFilesAsSiblings", "true");
        return result;
    }

    @Override
    protected Mirroring getMirroring(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor) {
        Mirroring mirroring = super.getMirroring(slice, monitor);
        mirroring.setIncludePacked(includePackedArtifacts);
        return mirroring;
    }
}
