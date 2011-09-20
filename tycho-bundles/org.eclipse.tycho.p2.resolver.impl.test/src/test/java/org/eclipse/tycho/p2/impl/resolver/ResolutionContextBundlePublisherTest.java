/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import static org.eclipse.tycho.p2.test.matcher.ArtifactRepositoryMatcher.containsEntry;
import static org.eclipse.tycho.p2.test.matcher.ArtifactRepositoryMatcher.entry;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasGAV;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasId;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasVersion;
import static org.eclipse.tycho.test.util.ResourceUtil.resourceFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.repository.test.util.LocalRepositoryStub;
import org.junit.Before;
import org.junit.Test;

public class ResolutionContextBundlePublisherTest {

    private static final String GROUP_ID = "org.eclipse.tycho.test.dummy";
    private static final String ARTIFACT_ID = "dummy-artifact";
    private static final String VERSION = "0.8.15-SNAPSHOT";

    private ResolutionContextBundlePublisher subject;
    private LocalRepositoryStub localRepo;

    @Before
    public void initSubject() {
        localRepo = new LocalRepositoryStub();
        subject = new ResolutionContextBundlePublisher(localRepo.getArtifactProvider(), new MavenLoggerStub(true, true));
    }

    @Test
    public void testPomDependencyOnBundle() throws Exception {
        String bundleId = "org.eclipse.osgi";
        String bundleVersion = "3.5.2.R35x_v20100126";

        File bundleFile = resourceFile("pom-dependencies/" + bundleId + "_" + bundleVersion + ".jar");
        IArtifactFacade bundleArtifact = new ArtifactMock(bundleFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");
        localRepo.addArtifact(bundleArtifact);

        IInstallableUnit publishedUnit = subject.attemptToPublishBundle(bundleArtifact);

        assertThat(publishedUnit, hasId(bundleId));
        assertThat(publishedUnit, hasVersion(bundleVersion));
        assertThat(publishedUnit, hasGAV(GROUP_ID, ARTIFACT_ID, VERSION));
        assertThat(publishedUnit.getArtifacts().size(), is(1));

        IArtifactKey artifactOfTheUnit = publishedUnit.getArtifacts().iterator().next();

        IArtifactRepository artifactRepo = subject.getArtifactRepoOfPublishedBundles();
        assertThat(artifactRepo, containsEntry(artifactOfTheUnit));
        assertThat(artifactRepo, entry(artifactOfTheUnit).hasContent(bundleFile));
    }

    @Test
    public void testPomDependencyOnPlainJar() throws Exception {
        File jarFile = resourceFile("pom-dependencies/non-bundle.jar");
        IArtifactFacade jarArtifact = new ArtifactMock(jarFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");

        IInstallableUnit unit = subject.attemptToPublishBundle(jarArtifact);

        assertNull(unit);
    }

    @Test
    public void testPomDependencyOnOtherType() throws Exception {
        File otherFile = resourceFile("pom-dependencies/other-type.xml");
        IArtifactFacade otherArtifact = new ArtifactMock(otherFile, GROUP_ID, ARTIFACT_ID, VERSION, "pom");

        IInstallableUnit unit = subject.attemptToPublishBundle(otherArtifact);

        assertNull(unit);
    }
}
