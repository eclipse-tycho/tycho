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
package org.eclipse.tycho.p2.tools.mirroring.facade;

import java.util.Collection;

import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;

public interface MirrorApplicationService {

    /**
     * Copies the given installable units and their dependencies into the p2 repository at the
     * destination location. By default this only includes the units and their dependencies with
     * strict versions (i.e. included content). Optionally, all transitive dependencies of the given
     * units are also copied, if includeAllDependencies is set to <code>true</code>.
     * 
     * @param sources
     *            The p2 repositories from which dependencies and artifacts are copied
     * @param destination
     *            The p2 repository that shall be written to. The location must be a directory,
     *            which may be empty. Existing content is not overwritten but is appended to.
     * @param seedUnits
     *            A set of installable units that span the content to be copied. Note that the given
     *            installable units are written into the destination p2 repository without checking
     *            if they are actually present in the source repositories. Therefore only units from
     *            the source repositories should be passed via this parameter.
     * @param context
     *            Build context information; in particular this parameter defines a filter for
     *            environment specific installable units
     * @param includeAllDependencies
     *            Whether to include all transitive dependencies
     * @throws FacadeException
     *             if a checked exception occurs while mirroring
     */
    public void mirrorReactor(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<?/* IInstallableUnit */> seedUnits, BuildContext context, boolean includeAllDependencies)
            throws FacadeException;

    /**
     * Copies installable units from the source repositories to the destination repository. The
     * corresponding artifacts are also copied unless the mirror options specify otherwise.
     * 
     * @param sources
     *            The p2 repositories from which content shall be copied.
     * @param destination
     *            The p2 repository that shall be written to. The location must be a directory,
     *            which may be empty. Existing content is not overwritten but is appended to.
     * @param seedUnits
     *            A set of installable units that span the content to be mirrored. May be
     *            <code>null</code> if all available IUs shall be copied. The given installable
     *            units will be checked if they are actually present in the source repositories.
     * @param mirrorOptions
     *            various mirror options. Must not be <code>null</code>.
     * @param tempDirectory
     *            A directory for storing temporary results. Typically the build target folder of a
     *            module.
     * @throws FacadeException
     *             if a checked exception occurs while mirroring
     */
    void mirrorStandalone(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<IUDescription> seedUnits, MirrorOptions mirrorOptions, BuildOutputDirectory tempDirectory)
            throws FacadeException;
}
