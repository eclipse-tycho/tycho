/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.ClassifiedLocation;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

public class PomDependencyCollectorImpl implements PomDependencyCollector {

    private final TargetPlatformBundlePublisher bundlesPublisher;
    private final MavenLogger logger;

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<IInstallableUnit, IArtifactFacade>();

    // TODO 412416 get rid of this field
    private File projectLocation;

    public PomDependencyCollectorImpl(MavenContext mavenContext) {
        this.logger = mavenContext.getLogger();

        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.bundlesPublisher = new TargetPlatformBundlePublisher(localRepositoryRoot, mavenContext.getLogger());
    }

    public void setProjectLocation(File projectLocation) {
        this.projectLocation = projectLocation;
    }

    public File getProjectLocation() {
        return projectLocation;
    }

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact) {
        IInstallableUnit bundleIU = bundlesPublisher.attemptToPublishBundle(artifact);
        if (bundleIU != null)
            addMavenArtifact(new ClassifiedLocation(artifact), artifact, Collections.singleton(bundleIU));
    }

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile) {
        try {
            addMavenArtifact(new ClassifiedLocation(artifact), artifact, readUnits(p2MetadataFile));
        } catch (IOException e) {
            throw new RuntimeException("failed to read p2 metadata", e);
        }
    }

    private Set<IInstallableUnit> readUnits(IArtifactFacade p2MetadataFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(p2MetadataFile.getLocation());
        try {
            MetadataIO io = new MetadataIO();
            return io.readXML(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public void addMavenArtifact(ClassifiedLocation key, IArtifactFacade artifact, Set<IInstallableUnit> units) {
        for (IInstallableUnit unit : units) {
            String classifier = unit.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
            if (classifier == null ? key.getClassifier() == null : classifier.equals(key.getClassifier())) {
                mavenInstallableUnits.put(unit, artifact);
                if (logger.isDebugEnabled()) {
                    logger.debug("P2Resolver: artifact "
                            + new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion())
                                    .toString() + " at location " + artifact.getLocation()
                            + " resolves installable unit " + new VersionedId(unit.getId(), unit.getVersion()));
                }
            }
        }
    }

    LinkedHashSet<IInstallableUnit> gatherMavenInstallableUnits() {
        return new LinkedHashSet<IInstallableUnit>(getMavenInstallableUnits().keySet());
    }

    Map<IInstallableUnit, IArtifactFacade> getMavenInstallableUnits() {
        return mavenInstallableUnits;
    }

    IRawArtifactFileProvider getArtifactRepoOfPublishedBundles() {
        return bundlesPublisher.getArtifactRepoOfPublishedBundles();
    }

}
