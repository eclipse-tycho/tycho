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
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.FileRepositoryArtifactProvider;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MetadataIO;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;

public class PomDependencyCollectorImpl implements PomDependencyCollector {

    private final Logger logger;

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<>();

    private Map<IArtifactFacade, IArtifactDescriptor> descriptorMap = new HashMap<>();
    private ReactorProject project;
    private final List<IArtifactDescriptor> fileDescriptors = new ArrayList<>();
    private FileRepositoryArtifactProvider fileRepositoryArtifactProvider;

    private FileArtifactRepository artifactRepository;

    public PomDependencyCollectorImpl(Logger logger, ReactorProject project, IProvisioningAgent agent) {
        this.logger = logger;
        this.project = project;
        artifactRepository = new FileArtifactRepository(agent, fileDescriptors::iterator);
        fileRepositoryArtifactProvider = new FileRepositoryArtifactProvider(
                Collections.singletonList(artifactRepository), ArtifactTransferPolicies.forLocalArtifacts());
    }

    public File getProjectLocation() {
        if (project != null) {
            return project.getBasedir();
        }
        return null;
    }

    @Override
    public Entry<ArtifactKey, IArtifactDescriptor> addMavenArtifact(IArtifactFacade artifact,
            Collection<IInstallableUnit> installableUnits) {
        Entry<ArtifactKey, IArtifactDescriptor> resultArtifactKey = null;
        for (IInstallableUnit unit : installableUnits) {
            mavenInstallableUnits.put(unit, artifact);
            for (IArtifactKey key : unit.getArtifacts()) {
                ArtifactKey artifactKey = ArtifactTypeHelper.toTychoArtifactKey(unit, key);
                if (artifactKey != null) {
                    IArtifactDescriptor descriptor = FileArtifactRepository.forFile(artifact.getLocation(), key,
                            artifactRepository);
                    fileDescriptors.add(descriptor);
                    descriptorMap.put(artifact, descriptor);
                    resultArtifactKey = new SimpleEntry<>(artifactKey, descriptor);
                }
            }
        }
        return resultArtifactKey;
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
        return fileRepositoryArtifactProvider;
    }

    @Override
    public ArtifactKey getArtifactKey(IArtifactFacade facade) {
        IArtifactDescriptor artifactDescriptor = descriptorMap.get(facade);
        if (artifactDescriptor == null) {
            //TODO should we simply use the packaging type here??
            String type;
            if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(facade.getPackagingType())) {
                type = ArtifactType.TYPE_ECLIPSE_FEATURE;
            } else {
                type = ArtifactType.TYPE_ECLIPSE_PLUGIN;
            }
            return new DefaultArtifactKey(type, facade.getArtifactId(), facade.getVersion());
        }
        IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, artifactKey.getId(),
                artifactKey.getVersion().toString());
    }

}
