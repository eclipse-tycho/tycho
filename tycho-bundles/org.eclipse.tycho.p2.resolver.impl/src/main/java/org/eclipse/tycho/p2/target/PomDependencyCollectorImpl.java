/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

public class PomDependencyCollectorImpl implements PomDependencyCollector {

    private final TargetPlatformBundlePublisher bundlesPublisher;
    private final MavenLogger logger;

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<>();
    private ReactorProject project;

    public PomDependencyCollectorImpl(MavenContext mavenContext, ReactorProject project) {
        this.project = project;
        this.logger = mavenContext.getLogger();

        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.bundlesPublisher = new TargetPlatformBundlePublisher(localRepositoryRoot, project,
                mavenContext.getLogger());
    }

    public File getProjectLocation() {
        if (project != null) {
            return project.getBasedir();
        }
        return null;
    }

    @Override
    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact) {
        MavenBundleInfo bundleIU = bundlesPublisher.attemptToPublishBundle(artifact, false);
        if (bundleIU != null) {
            addMavenArtifact(bundleIU.getArtifact(), Collections.singleton(bundleIU.getUnit()));
        }
    }

    @Override
    public void publishAndWrapArtifactIfNeccesary(IArtifactFacade artifact) {
        MavenBundleInfo bundleIU = bundlesPublisher.attemptToPublishBundle(artifact, true);
        if (bundleIU != null) {
            addMavenArtifact(bundleIU.getArtifact(), Collections.singleton(bundleIU.getUnit()));
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
            String classifier = unit.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
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

    Map<IInstallableUnit, IArtifactFacade> getMavenInstallableUnits() {
        return mavenInstallableUnits;
    }

    IRawArtifactFileProvider getArtifactRepoOfPublishedBundles() {
        return bundlesPublisher.getArtifactRepoOfPublishedBundles();
    }

}
