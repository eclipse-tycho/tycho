/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
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
 *                       - Issue #462 - Delay Pom considered items to the final Target Platform calculation 
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.repository.FileArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.FileRepositoryArtifactProvider;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class PomDependencyCollectorImpl implements PomDependencyCollector {

    private final TargetPlatformBundlePublisher bundlesPublisher;
    private final MavenLogger logger;

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<>();

    private Map<IArtifactFacade, IArtifactDescriptor> descriptorMap = new HashMap<>();
    private ReactorProject project;
    private IProvisioningAgent agent;
    private final List<IArtifactDescriptor> fileDescriptors = new ArrayList<>();
    private FileRepositoryArtifactProvider fileRepositoryArtifactProvider;

    public PomDependencyCollectorImpl(MavenContext mavenContext, ReactorProject project) {
        this.project = project;
        this.logger = mavenContext.getLogger();

        this.bundlesPublisher = new TargetPlatformBundlePublisher(project, mavenContext);
        try {
            agent = Activator.createProvisioningAgent(project == null ? null : project.getBasedir().toURI());
        } catch (ProvisionException e) {
        }
        fileRepositoryArtifactProvider = new FileRepositoryArtifactProvider(
                Collections.singletonList(new FileArtifactRepository(agent, () -> fileDescriptors.iterator())),
                ArtifactTransferPolicies.forLocalArtifacts());
    }

    public File getProjectLocation() {
        if (project != null) {
            return project.getBasedir();
        }
        return null;
    }

    @Override
    public void addMavenArtifact(IArtifactFacade artifact, boolean allowGenerateOSGiBundle) {
        if (artifact instanceof ReactorProjectFacade) {
            ReactorProjectFacade projectFacade = (ReactorProjectFacade) artifact;
            ReactorProject reactorProject = projectFacade.getReactorProject();
            String packaging = reactorProject.getPackaging();
            if ("bundle".equalsIgnoreCase(packaging) || "jar".equalsIgnoreCase(packaging)) {
                File artifactFile = reactorProject.getArtifact();
                if (artifactFile != null) {
                    try {
                        BundleDescription bundleDescription = BundlesAction.createBundleDescription(artifactFile);
                        if (bundleDescription != null && bundleDescription.getSymbolicName() != null) {
                            IArtifactKey key = BundlesAction.createBundleArtifactKey(
                                    bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
                            PublisherInfo publisherInfo = new PublisherInfo();
                            publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
                            IArtifactDescriptor descriptor = FileArtifactRepository.forFile(artifactFile, key);
                            fileDescriptors.add(descriptor);
                            IInstallableUnit iu = BundlesAction.createBundleIU(bundleDescription, key, publisherInfo);
                            mavenInstallableUnits.put(iu, projectFacade);
                            descriptorMap.put(projectFacade, descriptor);
                        } else {
                            if (allowGenerateOSGiBundle) {
                                logger.warn("Referenced reactor project " + projectFacade
                                        + " has no valid OSGi metadata and allowGenerateOSGiBundle=true is not applicable for reactor projects, consider adding felix- or bnd-maven-plugin to this project to generate appropriate OSGi headers in the first place.");
                            }
                        }
                    } catch (IOException | BundleException e) {
                        logger.warn("Referenced reactor project " + projectFacade + " could not be read!", e);
                    }
                }
            }
        } else {
            MavenBundleInfo bundleIU = bundlesPublisher.attemptToPublishBundle(artifact, allowGenerateOSGiBundle);
            if (bundleIU != null) {
                addMavenArtifact(bundleIU.getArtifact(), Collections.singleton(bundleIU.getUnit()));
                descriptorMap.put(bundleIU.getArtifact(), bundleIU.getDescriptor());
            }
        }
    }

    @Override
    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile) {
        try {
            addMavenArtifact(artifact, readUnits(p2MetadataFile));
        } catch (IOException e) {
            throw new RuntimeException("failed to read p2 metadata", e);
        }
    }

    private Set<IInstallableUnit> readUnits(IArtifactFacade p2MetadataFile) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(p2MetadataFile.getLocation())) {
            MetadataIO io = new MetadataIO();
            return io.readXML(inputStream);
        }
    }

    public void addMavenArtifact(IArtifactFacade artifact, Set<IInstallableUnit> units) {
        for (IInstallableUnit unit : units) {
            String classifier = unit.getProperty(TychoConstants.PROP_CLASSIFIER);
            if (Objects.equals(classifier, artifact.getClassifier())) {
                mavenInstallableUnits.put(unit, artifact);
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "P2Resolver: artifact "
                                    + new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion())
                                            .toString()
                                    + " at location " + artifact.getLocation() + " resolves installable unit "
                                    + new VersionedId(unit.getId(), unit.getVersion()));
                }
            } else if (logger.isDebugEnabled()) {
                logger.debug("P2Resolver: artifact "
                        + new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()).toString()
                        + " for installable unit " + new VersionedId(unit.getId(), unit.getVersion())
                        + " is ignored because of classifier missmatch");
            }
        }

    }

    LinkedHashSet<IInstallableUnit> gatherMavenInstallableUnits() {
        return new LinkedHashSet<>(getMavenInstallableUnits().keySet());
    }

    @Override
    public Map<IInstallableUnit, IArtifactFacade> getMavenInstallableUnits() {
        return mavenInstallableUnits;
    }

    IRawArtifactFileProvider getArtifactRepoOfPublishedBundles() {
        return new CompositeArtifactProvider(bundlesPublisher.getArtifactRepoOfPublishedBundles(),
                fileRepositoryArtifactProvider);
    }

    @Override
    public ArtifactKey getArtifactKey(IArtifactFacade facade) {
        IArtifactDescriptor artifactDescriptor = descriptorMap.get(facade);
        if (artifactDescriptor == null) {
            return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, facade.getArtifactId(),
                    facade.getVersion());
        }
        IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, artifactKey.getId(),
                artifactKey.getVersion().toString());
    }

}
