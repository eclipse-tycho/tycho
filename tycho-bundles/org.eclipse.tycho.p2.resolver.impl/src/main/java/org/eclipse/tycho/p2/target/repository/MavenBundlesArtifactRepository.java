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
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *                         Bug 567639 - wrapAsBundle fails when dealing with esoteric versions
 *                         Bug 567957 - wrapAsBundle must check if artifact has a classifier
 *                         Bug 568729 - Support new "Maven" Target location
 *******************************************************************************/
package org.eclipse.tycho.p2.target.repository;

import java.io.File;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.eclipse.tycho.repository.local.GAVArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;

/**
 * p2 artifact repository providing the POM dependency Maven artifacts.
 * 
 * <p>
 * Although the provided artifacts are also stored in the local Maven repository, they cannot be
 * made available via the <tt>LocalArtifactRepository</tt> artifact repository implementation. The
 * reason is that there are differences is how the artifacts provided by the respective
 * implementations may be updated:
 * <ul>
 * <li>For the <tt>LocalArtifactRepository</tt> artifacts, it can be assumed that all updates (e.g.
 * as a result of a <tt>mvn install</tt>) are done by Tycho. Therefore it is safe to write the p2
 * artifact index data to disk together with the artifacts.</li>
 * <li>For the POM dependency artifacts, this assumption does not hold true: e.g. a
 * maven-bundle-plugin build may update an artifact in the local Maven repository without notifying
 * Tycho. So if we had written p2 artifact index data to disk, that data might then be stale.</li>
 * </ul>
 * To avoid the need to implement and index invalidation logic, we use this separate artifact
 * repository implementation with an in-memory index.
 * </p>
 */
public final class MavenBundlesArtifactRepository extends ArtifactRepositoryBaseImpl<GAVArtifactDescriptor> {

    private MavenContext mavenContext;

    public MavenBundlesArtifactRepository(MavenContext mavenContext) {
        super(null, mavenContext.getLocalRepositoryRoot().toURI(), ArtifactTransferPolicies.forLocalArtifacts());
        this.mavenContext = mavenContext;
    }

    public void addPublishedArtifact(IArtifactDescriptor baseDescriptor, IArtifactFacade mavenArtifact) {
        // TODO allow other extensions than the default ("jar")?
        MavenRepositoryCoordinates repositoryCoordinates = new MavenRepositoryCoordinates(mavenArtifact.getGroupId(),
                mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), mavenArtifact.getClassifier(), null);

        GAVArtifactDescriptor descriptorForRepository = new GAVArtifactDescriptor(baseDescriptor,
                repositoryCoordinates);

        File requiredArtifactLocation = new File(getBaseDir(),
                descriptorForRepository.getMavenCoordinates().getLocalRepositoryPath(mavenContext));
        File actualArtifactLocation = mavenArtifact.getLocation();
        if (!equivalentPaths(requiredArtifactLocation, actualArtifactLocation)) {
            throw new AssertionFailedException(
                    "The Maven artifact to be added to the target platform is not stored at the required location on disk: required \""
                            + requiredArtifactLocation + "\" but was \"" + actualArtifactLocation + "\"");
        }

        internalAddInternalDescriptor(descriptorForRepository);
    }

    private boolean equivalentPaths(File path, File otherPath) {
        return path.equals(otherPath);
    }

    @Override
    protected GAVArtifactDescriptor getInternalDescriptorForAdding(IArtifactDescriptor descriptor) {
        // artifacts are only added via the dedicated method
        throw new UnsupportedOperationException();
    }

    @Override
    protected IArtifactDescriptor getComparableDescriptor(IArtifactDescriptor descriptor) {
        // any descriptor can be converted to our internal type GAVArtifactDescriptor
        return toInternalDescriptor(descriptor);
    }

    private GAVArtifactDescriptor toInternalDescriptor(IArtifactDescriptor descriptor) {
        // TODO share with LocalArtifactRepository?
        if (descriptor instanceof GAVArtifactDescriptor && descriptor.getRepository() == this) {
            return (GAVArtifactDescriptor) descriptor;
        } else {
            GAVArtifactDescriptor internalDescriptor = new GAVArtifactDescriptor(descriptor);
            internalDescriptor.setRepository(this);
            return internalDescriptor;
        }
    }

    @Override
    protected File internalGetArtifactStorageLocation(IArtifactDescriptor descriptor) {
        String relativePath = toInternalDescriptor(descriptor).getMavenCoordinates()
                .getLocalRepositoryPath(mavenContext);
        return new File(getBaseDir(), relativePath);
    }

    public File getBaseDir() {
        return new File(getLocation());
    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifact) {
        return contains(artifact);
    }
}
