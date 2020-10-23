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
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.impl.publisher.repo.TransientArtifactRepository;
import org.eclipse.tycho.p2.metadata.ArtifactFacadeProxy;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.eclipse.tycho.repository.local.GAVArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;
import org.eclipse.tycho.repository.util.StatusTool;
import org.osgi.framework.BundleException;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;

@SuppressWarnings("restriction")
public class TargetPlatformBundlePublisher {

    private static final String WRAPPED_CLASSIFIER = "wrapped";

    private final MavenLogger logger;
    private final PublishedBundlesArtifactRepository publishedArtifacts;
    private ReactorProject project;

    public TargetPlatformBundlePublisher(File localMavenRepositoryRoot, ReactorProject project, MavenLogger logger) {
        this.project = project;
        this.publishedArtifacts = new PublishedBundlesArtifactRepository(localMavenRepositoryRoot);
        this.logger = logger;
    }

    /**
     * Generate p2 data for an artifact, if the artifact is an OSGI bundle.
     * <p>
     * The p2 metadata produced by this method is only determined by the artifact, and the function
     * used for this conversion must not change (significantly) even in future versions. This is
     * required because the resulting metadata can be included in p2 repositories built by Tycho,
     * and hence may be propagated into the p2 universe. Therefore the metadata generated by this
     * method shall fulfill the basic assumption of p2 that ID+version uniquely identifies a
     * unit/artifact. Assuming that distinct bundle artifacts specify unique ID+versions in their
     * manifest (which should be mostly true), and the p2 BundlesAction used in the implementation
     * doesn't change significantly (which can also be assumed), these conditions specified above a
     * met.
     * </p>
     * <p>
     * In slight deviation on the principles described in the previous paragraph, the implementation
     * adds GAV properties to the generated IU. This is justified by the potential benefits of
     * tracing the origin of artifact.
     * </p>
     * 
     * @param mavenArtifact
     *            An artifact in local file system.
     * @return the p2 metadata of the artifact, or <code>null</code> if the artifact isn't a valid
     *         OSGi bundle.
     */
    MavenBundleInfo attemptToPublishBundle(IArtifactFacade mavenArtifact, boolean wrapIfNessesary) {
        if (!isAvailableAsLocalFile(mavenArtifact)) {
            // this should have been ensured by the caller
            throw new IllegalArgumentException("Not an artifact file: " + mavenArtifact.getLocation());
        }
        PublisherRun publisherRun = new PublisherRun(mavenArtifact, project, publishedArtifacts.getBaseDir(), logger,
                wrapIfNessesary);
        IStatus status = publisherRun.execute();
        if (!status.isOK()) {
            /**
             * If publishing of a jar fails, it is simply not added to the resolution context. The
             * BundlesAction already ignores non-bundle JARs silently, so an error status here
             * indicates a caught exception that we at least want to see.
             */
            logger.warn(StatusTool.collectProblems(status), status.getException());
        }

        MavenBundleInfo publishedIU = publisherRun.getPublishedUnitIfExists();
        if (publishedIU != null) {
            publishedArtifacts.addPublishedArtifact(publishedIU.getDescriptor(), publishedIU.getArtifact());
        }

        return publishedIU;
    }

    private boolean isAvailableAsLocalFile(IArtifactFacade artifact) {
        File localLocation = artifact.getLocation();
        return localLocation != null && localLocation.isFile();
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
        private IArtifactFacade publishedArtifact;

        private ReactorProject project;

        private File basedir;

        private MavenLogger logger;

        private boolean wrapIfNessesary;

        PublisherRun(IArtifactFacade artifact, ReactorProject project, File basedir, MavenLogger logger,
                boolean wrapIfNessesary) {
            this.mavenArtifact = artifact;
            this.project = project;
            this.basedir = basedir;
            this.logger = logger;
            this.wrapIfNessesary = wrapIfNessesary;
        }

        IStatus execute() {
            try {
                BundleDescription bundleDescription = BundlesAction
                        .createBundleDescription(mavenArtifact.getLocation());
                if (bundleDescription == null) {
                    return new Status(IStatus.OK, TargetPlatformBundlePublisher.class.getName(),
                            "artifact file " + mavenArtifact.getLocation() + " is certainly not a bundle/jar file");
                }
                if (bundleDescription.getSymbolicName() == null) {
                    if (wrapIfNessesary) {
                        String generatedBsn = project.getGroupId() + "." + mavenArtifact.getGroupId() + "."
                                + mavenArtifact.getArtifactId();
                        String classifier = mavenArtifact.getClassifier();
                        String wrappedClassifier = WRAPPED_CLASSIFIER;

                        if (classifier != null && !classifier.isEmpty()) {
                            generatedBsn = generatedBsn + "." + classifier;
                            wrappedClassifier = classifier + "-" + WRAPPED_CLASSIFIER;
                        }
                        logger.warn("Maven Artifact " + mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId()
                                + ":" + mavenArtifact.getVersion()
                                + " is not a bundle a will be automatically wrapped with bundle-symbolic name "
                                + generatedBsn
                                + ", ignoring such artifacts can be enabled with <pomDependencies>consider</pomDependencies> in target platform configuration.");
                        try {
                            publishedArtifact = createWrappedArtifact(generatedBsn, wrappedClassifier);
                        } catch (Exception e) {
                            return new Status(IStatus.ERROR, TargetPlatformBundlePublisher.class.getName(),
                                    "wrapping file " + mavenArtifact.getLocation() + " failed", e);
                        }
                    } else {
                        logger.info("Maven Artifact " + mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId()
                                + ":" + mavenArtifact.getVersion()
                                + " is not a bundle and will be ignored, automatic wrapping of such artifacts can be enabled with <pomDependencies>use</pomDependencies> in target platform configuration.");
                        return new Status(IStatus.OK, TargetPlatformBundlePublisher.class.getName(), "Nothing to do");
                    }

                } else {
                    publishedArtifact = mavenArtifact;
                }
            } catch (IOException e) {
                return new Status(IStatus.WARNING, TargetPlatformBundlePublisher.class.getName(),
                        "reading file " + mavenArtifact.getLocation() + " failed", e);
            } catch (BundleException e) {
                return new Status(IStatus.WARNING, TargetPlatformBundlePublisher.class.getName(),
                        "reading maven manifest from file: " + mavenArtifact.getLocation() + " failed", e);
            }

            publisherInfo = new PublisherInfo();
            enableArtifactDescriptorCollection();
            enableUnitAnnotationWithGAV();

            BundlesAction bundlesAction = new BundlesAction(new File[] { publishedArtifact.getLocation() });
            IStatus status = executePublisherAction(bundlesAction);
            return status;
        }

        private void enableArtifactDescriptorCollection() {
            publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
            collectedDescriptors = new TransientArtifactRepository();
            publisherInfo.setArtifactRepository(collectedDescriptors);
        }

        private void enableUnitAnnotationWithGAV() {
            MavenPropertiesAdvice advice = new MavenPropertiesAdvice(publishedArtifact.getGroupId(),
                    publishedArtifact.getArtifactId(), publishedArtifact.getVersion(),
                    publishedArtifact.getClassifier());
            publisherInfo.addAdvice(advice);
        }

        private IStatus executePublisherAction(BundlesAction action) {
            IPublisherAction[] actions = new IPublisherAction[] { action };
            publisherResult = new PublisherResult();
            return new Publisher(publisherInfo, publisherResult).publish(actions, null);
        }

        MavenBundleInfo getPublishedUnitIfExists() {
            if (publisherResult == null) {
                return null;
            }
            Collection<IInstallableUnit> units = publisherResult.getIUs(null, null);
            if (units.isEmpty()) {
                // the BundlesAction simply does not create any IUs if the JAR is not a bundle
                return null;
            } else if (units.size() == 1) {
                IInstallableUnit unit = units.iterator().next();
                IArtifactDescriptor artifactDescriptor = getPublishedArtifactDescriptor();
                return new MavenBundleInfo(unit, artifactDescriptor, publishedArtifact);
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

        private IArtifactFacade createWrappedArtifact(String bsn, String classifier) throws Exception {
            MavenRepositoryCoordinates repositoryCoordinates = new MavenRepositoryCoordinates(
                    mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), classifier,
                    null);
            File wrappedFile = new File(basedir, repositoryCoordinates.getLocalRepositoryPath());
            wrappedFile.getParentFile().mkdirs();
            try (Jar jar = new Jar(mavenArtifact.getLocation())) {
                Manifest originalManifest = jar.getManifest();
                try (Analyzer analyzer = new Analyzer();) {
                    analyzer.setJar(jar);
                    if (originalManifest != null) {
                        analyzer.mergeManifest(originalManifest);
                    }
                    Version version = createOSGiVersionFromArtifact(mavenArtifact);
                    analyzer.setProperty(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");
                    analyzer.setProperty(Analyzer.EXPORT_PACKAGE, "*;version=\"" + version + "\";-noimport:=true");
                    analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, bsn);
                    analyzer.setBundleVersion(version);
                    Manifest manifest = analyzer.calcManifest();
                    jar.setManifest(manifest);
                    jar.write(wrappedFile);
                    if (logger.isDebugEnabled()) {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        manifest.write(bout);
                        logger.debug("Generated Manifest: \r\n" + bout.toString(StandardCharsets.UTF_8));
                    }
                    return new WrappedArtifact(wrappedFile, mavenArtifact, classifier);
                }
            }
        }
    }

    public static Version createOSGiVersionFromArtifact(IArtifactFacade artifact) {
        String version = artifact.getVersion();
        try {
            int index = version.indexOf('-');
            if (index > -1) {
                StringBuilder sb = new StringBuilder(version);
                sb.setCharAt(index, '.');
                return Version.parseVersion(sb.toString());
            }
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            return new Version(0, 0, 1, version);
        }
    }

    private static final class WrappedArtifact extends ArtifactFacadeProxy {

        private final File file;
        private final String classifier;

        public WrappedArtifact(File file, IArtifactFacade wrapped, String classifier) {
            super(wrapped);
            this.file = file;
            this.classifier = classifier;
        }

        @Override
        public File getLocation() {
            return file;
        }

        @Override
        public String getClassifier() {
            return classifier;
        }

        @Override
        public String getPackagingType() {
            return "bundle";
        }

        @Override
        public String toString() {
            return "WrappedArtifact [file=" + file + ", wrapped=" + super.toString() + ", classifier=" + classifier
                    + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(classifier, file);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            WrappedArtifact other = (WrappedArtifact) obj;
            return Objects.equals(classifier, other.classifier) && Objects.equals(file, other.file);
        }

    }

    /**
     * p2 artifact repository providing the POM dependency Maven artifacts.
     * 
     * <p>
     * Although the provided artifacts are also stored in the local Maven repository, they cannot be
     * made available via the <tt>LocalArtifactRepository</tt> artifact repository implementation.
     * The reason is that there are differences is how the artifacts provided by the respective
     * implementations may be updated:
     * <ul>
     * <li>For the <tt>LocalArtifactRepository</tt> artifacts, it can be assumed that all updates
     * (e.g. as a result of a <tt>mvn install</tt>) are done by Tycho. Therefore it is safe to write
     * the p2 artifact index data to disk together with the artifacts.</li>
     * <li>For the POM dependency artifacts, this assumption does not hold true: e.g. a
     * maven-bundle-plugin build may update an artifact in the local Maven repository without
     * notifying Tycho. So if we had written p2 artifact index data to disk, that data might then be
     * stale.</li>
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

        @Override
        public boolean isFileAlreadyAvailable(IArtifactKey artifact) {
            return contains(artifact);
        }
    }

}
