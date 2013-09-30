/*******************************************************************************
 * Copyright (c) 2011, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - Moved tests to a separate class; refactorings
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PomDependencyCollectorTest {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private PomDependencyCollector subject;

    private ArtifactMock artifact;

    @Before
    public void setUpSubject() throws Exception {
        MavenContextImpl mavenContext = new MavenContextImpl(new File("dummy"), logVerifier.getLogger());
        subject = new PomDependencyCollectorImpl(mavenContext);
    }

    @Test
    public void testAddArtifactWithExistingMetadataRespectsClassifier() throws Exception {
        // classifier matches one of the two IUs
        artifact = artifactWithClassifier("sources");

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = getTargetPlatformUnits();
        assertThat(units, hasItem(unitWithId("test.ui.source")));
        assertThat(units.size(), is(1));
    }

    @Test
    public void testAddArtifactWithExistingMetadataOfMainArtifact() throws Exception {
        // main (i.e. null) classifier matches one of the two IUs
        artifact = artifactWithClassifier(null);

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = getTargetPlatformUnits();
        assertThat(units, hasItem(unitWithId("test.ui")));
        assertThat(units.size(), is(1));
    }

    @Test
    public void testAddArtifactWithExistingMetadataButNonMatchingClassifier() throws Exception {
        // classifier does not match any of the available metadata
        artifact = artifactWithClassifier("classifier-not-in-p2-metadata");

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = getTargetPlatformUnits();
        assertThat(units.size(), is(0));
    }

    static ArtifactMock artifactWithClassifier(String classifier) throws Exception {
        return new ArtifactMock(new File(
                "resources/platformbuilder/pom-dependencies/org.eclipse.osgi_3.5.2.R35x_v20100126.jar"), "groupId",
                "artifactId", "1", ArtifactKey.TYPE_ECLIPSE_PLUGIN, classifier);
    }

    static ArtifactMock existingMetadata() {
        return new ArtifactMock(new File("resources/platformbuilder/pom-dependencies/existing-p2-metadata.xml"),
                "groupId", "artifactId", "1", ArtifactKey.TYPE_ECLIPSE_PLUGIN, "p2metadata");
    }

    private Collection<IInstallableUnit> getTargetPlatformUnits() {
        TestResolverFactory resolverFactory = new TestResolverFactory(logVerifier.getLogger());
        P2TargetPlatform platform = resolverFactory.getTargetPlatformFactoryImpl().createTargetPlatform(
                new TargetPlatformConfigurationStub(), NOOP_EE_RESOLUTION_HANDLER, null, subject);
        return platform.getInstallableUnits();
    }
}
