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

import java.util.Collection;

import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;

public interface MirrorApplicationService {
    /**
     * Flag to indicate the repository metadata files (content.xml, artifacts.xml) of newly created
     * p2 repositories shall be compressed.
     */
    public static final int REPOSITORY_COMPRESS = 1;

    /**
     * Flag to indicate that the target p2 repository shall include all transitive dependencies of
     * the specified root IUs. This yields a self-contained repository.
     */
    public static final int INCLUDE_ALL_DEPENDENCIES = 2;

    /**
     * Flag to indicate whether all referenced artifacts shall be mirrored into an artifact
     * repository. If this flag is not set, only the installable units are mirrored (and hence only
     * a metadata repository is created).
     */
    public static final int MIRROR_ARTIFACTS = 4;

    /**
     * Copies the given installable units and their dependencies into the p2 repository at the
     * destination location. By default this only includes the units and their dependencies with
     * strict versions (i.e. included content). Optionally, the following additional content is
     * copied:
     * <ul>
     * <li>all transitive dependencies of the given units, if {@link #INCLUDE_ALL_DEPENDENCIES} is
     * set</li>
     * <li>all referenced artifacts, if {@link #MIRROR_ARTIFACTS} is set</li>
     * </ul>
     * 
     * @param sources
     *            The p2 repositories from which dependencies and artifacts are copied
     * @param destination
     *            The p2 repository that shall be written to. The location must be a directory,
     *            which may be empty. Existing content is not overwritten but is appended to.
     * @param rootUnits
     *            A set of installable units that span the content to be copied. Note that the given
     *            installable units are written into the destination p2 repository without checking
     *            if they are actually present in the source repositories. Therefore only units from
     *            the source repositories should be passed via this parameter. Alternatively, this
     *            parameter may be <code>null</code> to mirror all units available in the sources.
     * @param context
     *            Build context information; in particular this parameter defines a filter for
     *            environment specific installable units
     * @param flags
     *            Additional options. flag is a <em>bitwise OR</em>'ed combination of
     *            {@link #MIRROR_ARTIFACTS}, {@link #INCLUDE_ALL_DEPENDENCIES},
     *            {@link #REPOSITORY_COMPRESS}
     * @throws FacadeException
     *             if a checked exception occurs while mirroring
     */
    public void mirror(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<?/* IInstallableUnit */> rootUnits, BuildContext context, int flags) throws FacadeException;
}
