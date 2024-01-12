/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;

import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

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
     * Key under which the preliminary target platform is stored in the reactor project instances.
     */
    String PRELIMINARY_TARGET_PLATFORM_KEY = "org.eclipse.tycho.core.TychoConstants/dependencyOnlyTargetPlatform";

    /**
     * Returns an artifact of the given type, id and matching version. The version reference string
     * matches versions according to the following rules:
     * <ul>
     * <li>"0.0.0" or <code>null</code> matches any version
     * <li>"1.2.3.qualifier", i.e. a version with a literal "qualifier", matches all versions in the
     * range [1.2.3,1.2.4)
     * <li>all other version references match artifacts with exactly that version. For example the
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
     *            May be <code>null</code>.
     * @return a matching artifact.
     * @throws IllegalArtifactReferenceException
     *             if an invalid type or malformed version reference is given
     * @throws DependencyResolutionException
     *             if there is no matching artifact in the target platform.
     */
    // TODO For the final TP, all versions are expanded - but ArtifactKey specifies that contains versions with non-expanded qualifiers; use a different type?
    ArtifactKey resolveArtifact(String type, String id, String versionRef)
            throws IllegalArtifactReferenceException, DependencyResolutionException;

    /**
     * Returns the file system location of the given target platform artifact.
     * 
     * @return the location of the given artifact, or <code>null</code> if the artifact does not
     *         exist in the target platform, or if the given <tt>ArtifactKey</tt> refers to an
     *         metadata-only "artifact" e.g. a product definition.
     */
    File getArtifactLocation(ArtifactKey artifact);

    boolean isFileAlreadyAvailable(ArtifactKey artifactKey);

    default ResolvedArtifactKey resolvePackage(String packageName, String versionRef)
            throws DependencyResolutionException, IllegalArtifactReferenceException {
        ArtifactKey packageJar = resolveArtifact(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, packageName, versionRef);
        File location = getArtifactLocation(
                new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, packageJar.getId(), packageJar.getVersion()));
        return ResolvedArtifactKey.of(ArtifactType.TYPE_ECLIPSE_PLUGIN, packageJar.getId(), packageJar.getVersion(),
                location);
    }

    /**
     * @return the target platform content as a {@link IMetadataRepository}.
     */
    IMetadataRepository getMetadataRepository();

    /**
     * @return the target platform content as a {@link IArtifactRepository}.
     */
    IArtifactRepository getArtifactRepository();

}
