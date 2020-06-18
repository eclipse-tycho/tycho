/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Bug 538144] Support other target locations (Directory, Features, Installations) 
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Result from resolving a target definition: A list of installable units, plus the URLs of the
 * artifact repositories containing the corresponding artifacts.
 */
public interface TargetDefinitionContent {

    /**
     * 
     * @return a queryable that returns all units selected by this {@link TargetDefinitionContent}
     *         the default implementation simply returns the metadata repository
     */
    default IQueryable<IInstallableUnit> getUnits() {
        return getMetadataRepository();
    }

    /**
     * 
     * @return the metadata repository that contains all metadata available for this
     *         {@link TargetDefinitionContent}
     */
    IMetadataRepository getMetadataRepository();

    /**
     * 
     * @return the artifact repository that could be used to resolve artifacts from this
     *         {@link TargetDefinitionContent}
     */
    IArtifactRepository getArtifactRepository();
}
