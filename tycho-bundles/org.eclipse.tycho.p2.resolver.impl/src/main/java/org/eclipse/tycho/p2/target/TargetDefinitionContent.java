/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Bug 538144] Support other target locations (Directory, Features, Installations) 
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Result from resolving a target definition: A list of installable units, plus the URLs of the
 * artifact repositories containing the corresponding artifacts.
 */
public interface TargetDefinitionContent extends IQueryable<IInstallableUnit> {

    /**
     * Allows to query for all units currently selected by this {@link TargetDefinitionContent} the
     * default implementation simply returns the metadata repository
     */
    @Override
    default IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return getMetadataRepository().query(query, monitor);
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
