/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.impl.publisher.repo.TransientArtifactRepository;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.eclipse.tycho.repository.local.GAVArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
public class TargetPlatformBundlePublisher {

    private final MavenLogger logger;
    private final PublishedBundlesArtifactRepository publishedArtifacts;

    public TargetPlatformBundlePublisher(File localMavenRepositoryRoot, MavenLogger logger) {
        this.publishedArtifacts = new PublishedBundlesArtifactRepository(localMavenRepositoryRoot);
        this.logger = logger;
    }

    /**
     * Generate p2 data for an artifact, if the artifact is an OSGI bundle.
     * <p>
     * The p2 metadata produced by this method is only determined by the artifact, and the function used
     * for this conversion must not change (significantly) even in future versions. This is required
     * because the resulting metadata can be included in p2 repositories built by Tycho, and hence may
     * be propagated into the p2 universe. Therefore the metadata generated by this method shall fulfill
     * the basic assumption of p2 that ID+version uniquely identifies a unit/artifact. Assuming that
     * distinct bundle artifacts specify unique ID+versions in their manifest (which should be mostly
     * true), and the p2 BundlesAction used in the implementation doesn't change significantly (which
     * can also be assumed), these conditions specified above a met.
     * </p>
     * <p>
     * In slight deviation on the principles described in the previous paragraph, the implementation
     * adds GAV properties to the generated IU. This is justified by the potential benefits of tracing
     * the origin of artifact.
     * </p>
     * 
     * @param mavenArtifact
     *            An artifact in local file system.
     * @return the p2 metadata of the artifact, or <code>null</code> if the artifact isn't a valid OSGi
     *         bundle.
     */
    IInstallableUnit attemptToPublishBundle(IArtifactFacade mavenArtifact) {
        if (!isAvailableAsLocalFile(mavenArtifact)) {
            // this should have been ensured by the caller
            throw new IllegalArgumentException("Not an artifact file: " + mavenArtifact.getLocation());
        }
        if (isCertainlyNoBundle(mavenArtifact)) {
            return null;
        }

        PublisherRun publisherRun = new PublisherRun(mavenArtifact);
        IStatus status = publisherRun.execute();

        if (!status.isOK()) {
            /**
             * If publishing of a jar fails, it is simply not added to the resolution context. The BundlesAction
             * already ignores non-bundle JARs silently, so an error status here indicates a caught exception
             * that we at least want to see.
             */
            logger.warn(StatusTool.collectProblems(status), status.getException());
        }

        IInstallableUnit publishedIU = publisherRun.getPublishedUnitIfExists();
        if (publishedIU != null) {
            IArtifactDescriptor publishedDescriptor = publisherRun.getPublishedArtifactDescriptor();
            publishedArtifacts.addPublishedArtifact(publishedDescriptor, mavenArtifact);
        }

        return publishedIU;
    }

    private boolean isAvailableAsLocalFile(IArtifactFacade artifact) {
        File localLocation = artifact.getLocation();
        return localLocation != null && localLocation.isFile();
    }

    private boolean isCertainlyNoBundle(IArtifactFacade artifact) {
        return !artifact.getLocation().getName().endsWith(".jar");
    }

    IRawArtifactFileProvider getArtifactRepoOfPublishedBundles() {
        return publishedArtifacts;
    }

    private static class PublisherRun {

        private static final String EXCEPTION_CONTEXT = "Error while adding Maven artifact to the target platform: ";

        private final IArtifactFacade mavenArtifact;

        private PublisherInfo publisherInfo;
        private TransientArtifactRepository collectedDescriptors;
        private PublisherResult publisherResult;

        PublisherRun(IArtifactFacade artifact) {
            this.mavenArtifact = artifact;
        }

        IStatus execute() {
            publisherInfo = new PublisherInfo();
            // Skip checksum generation as this is very costly and is not required in the dependency resolution scenario
            publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_NO_MD5);
            enableArtifactDescriptorCollection();
            enableUnitAnnotationWithGAV();

            BundlesAction bundlesAction = new BundlesAction(new File[] { mavenArtifact.getLocation() });
            IStatus status = executePublisherAction(bundlesAction);
            return status;
        }

        private void enableArtifactDescriptorCollection() {
            collectedDescriptors = new TransientArtifactRepository();
            publisherInfo.setArtifactRepository(collectedDescriptors);
        }

        private void enableUnitAnnotationWithGAV() {
            MavenPropertiesAdvice advice = new MavenPropertiesAdvice(mavenArtifact.getGroupId(),
                    mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), mavenArtifact.getClassifier());
            publisherInfo.addAdvice(advice);
        }

        private IStatus executePublisherAction(BundlesAction action) {
            IPublisherAction[] actions = new IPublisherAction[] { action };
            publisherResult = new PublisherResult();
            return new Publisher(publisherInfo, publisherResult).publish(actions, null);
        }

        IInstallableUnit getPublishedUnitIfExists() {
            Collection<IInstallableUnit> units = publisherResult.getIUs(null, null);
            if (units.isEmpty()) {
                // the BundlesAction simply does not create any IUs if the JAR is not a bundle
                return null;
            } else if (units.size() == 1) {
                return units.iterator().next();
            } else {
                throw new AssertionFailedException(EXCEPTION_CONTEXT + "BundlesAction produced more than one IU for "
                        + mavenArtifact.getLocation());
            }
        }

        IArtifactDescriptor getPublishedArtifactDescriptor() {
            Set<IArtifactDescriptor> descriptors = collectedDescriptors.getArtifactDescriptors();
            if (descriptors.isEmpty()) {
                throw new AssertionFailedException(EXCEPTION_CONTEXT
                        + "BundlesAction did not create an artifact entry for " + mavenArtifact.getLocation());
            } else if (descriptors.size() == 1) {
                return descriptors.iterator().next();
            } else {
                throw new AssertionFailedException(EXCEPTION_CONTEXT
                        + "BundlesAction created more than one artifact entry for " + mavenArtifact.getLocation());
            }
        }
    }

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
    private static class PublishedBundlesArtifactRepository extends ArtifactRepositoryBaseImpl<GAVArtifactDescriptor> {

        PublishedBundlesArtifactRepository(File localMavenRepositoryRoot) {
            super(null, localMavenRepositoryRoot.toURI(), ArtifactTransferPolicies.forLocalArtifacts());
        }

        void addPublishedArtifact(IArtifactDescriptor baseDescriptor, IArtifactFacade mavenArtifact) {
            // TODO allow other extensions than the default ("jar")?
            MavenRepositoryCoordinates repositoryCoordinates = new MavenRepositoryCoordinates(
                    mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion(),
                    mavenArtifact.getClassifier(), null);

            GAVArtifactDescriptor descriptorForRepository = new GAVArtifactDescriptor(baseDescriptor,
                    repositoryCoordinates);

            File requiredArtifactLocation = new File(getBaseDir(),
                    descriptorForRepository.getMavenCoordinates().getLocalRepositoryPath());
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
            String relativePath = toInternalDescriptor(descriptor).getMavenCoordinates().getLocalRepositoryPath();
            return new File(getBaseDir(), relativePath);
        }

        private File getBaseDir() {
            return new File(getLocation());
        }

    }
}
