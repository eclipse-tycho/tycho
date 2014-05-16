/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.publishing;

import java.io.File;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.registry.facade.PublishingRepositoryFacade;

/**
 * Representation of the p2 repositories that receive the artifacts produced by a project.
 */
public interface PublishingRepository extends PublishingRepositoryFacade {

    /**
     * Returns the project for which this instance contains the publishing results.
     */
    public ReactorProjectIdentities getProjectIdentities();

    public IMetadataRepository getMetadataRepository();

    public IRawArtifactFileProvider getArtifacts();

    public IFileArtifactRepository getArtifactRepository();

    /**
     * Returns a view onto the project's artifact repository which allows writing new artifacts.
     * 
     * @param writeSession
     *            a callback used to assign (Maven) classifiers to the new (p2) artifacts.
     */
    IArtifactRepository getArtifactRepositoryForWriting(WriteSessionContext writeSession);

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
