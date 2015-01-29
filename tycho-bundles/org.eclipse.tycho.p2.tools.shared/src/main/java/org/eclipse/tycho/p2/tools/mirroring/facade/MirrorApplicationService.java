/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring.facade;

import java.util.Collection;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;

/**
 * Facade to the p2 mirror application.
 * 
 * Note: The p2 mirror application is no longer used in Tycho core. Instead, Tycho now has it's own
 * implementation for p2 metadata copying - see {@link RepositoryAggregator}.
 */
public interface MirrorApplicationService {

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
