/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts;

import java.io.File;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;

/**
 * Set of artifacts which can be used by the build of a project, e.g. to resolve the project's
 * dependencies.
 * 
 * In current Tycho (cf. bug 353889), first a preliminary target platform is computed for each
 * reactor project to determine the reactor build order. Then, the final target platform is computed
 * as a normal build step. Note that only the final target platform must be used to produce build
 * results. The preliminary target platform contains partial information ("dependency-only IUs")
 * about the reactor artifacts, which must not end up in the build results.
 */
public interface TargetPlatform {
    /**
     * Key under which the final target platform is stored in the reactor project instances.
     */
    String FINAL_TARGET_PLATFORM_KEY = "org.eclipse.tycho.core.TychoConstants/targetPlatform";

    /**
     * Returns an artifact of the given type, id and matching version. The version reference string
     * matches versions according to the following rules:
     * <ul>
     * <li>"0.0.0" or <code>null</code> matches any version
     * <li>"1.2.3.qualifier", i.e. versions with a literal "qualifier", matches all versions in the
     * range [1.2.3,1.2.4)
     * <li>all other version references match artifact with exactly that version. For example the
     * version reference "1.2.3.v2014" stands for the strict version range [1.2.3.v2014,1.2.3.v2014]
     * </ul>
     * 
     * In case there multiple matching artifacts, the artifact with the highest version is returned.
     * 
     * @param type
     *            One of the types defined in {@link ArtifactType}
     * @param id
     *            The ID of the artifact to be found.
     * @param versionRef
     *            A version reference string selecting one exact version or versions from a range.
     * @return an artifact matching the referencing, or <code>null</code> if there is no such
     *         artifact in the target platform.
     * @throws IllegalArtifactReferenceException
     *             if an invalid type or malformed version reference is given
     */
    // TODO For the final TP, all versions are expanded - but ArtifactKey specifies that contains versions with non-expanded qualifiers; use a different type?
    ArtifactKey resolveReference(String type, String id, String versionRef) throws IllegalArtifactReferenceException;

    /**
     * Returns the file system location of the given target platform artifact. Not supported by the
     * preliminary target platform.
     */
    File getArtifactLocation(ArtifactKey artifact);

}
