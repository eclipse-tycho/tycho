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
package org.eclipse.tycho.repository.publishing;

import java.io.File;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.repository.registry.facade.PublishingRepositoryFacade;

/**
 * Representation of the p2 repository that receive the artifacts produced by the build.
 */
public interface PublishingRepository extends PublishingRepositoryFacade {

    public IMetadataRepository getMetadataRepository();

    public IArtifactRepository getArtifactRepository();

    /**
     * Adds the location of an existing artifact. This method should be called for artifacts which
     * are not packed by a publisher action.
     * 
     * @param classifier
     *            the classifier of the artifact, or <code>null</code> for the main artifact.
     * @param artifactLocation
     *            the location of the artifact
     */
    void addArtifactLocation(String classifier, File artifactLocation) throws ProvisionException;

}
