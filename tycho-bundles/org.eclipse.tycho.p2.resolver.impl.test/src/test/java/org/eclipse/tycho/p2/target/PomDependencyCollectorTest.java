/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - Moved tests to a separate class; refactorings
 *    Christoph Läubrich - Adjust to API
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.impl.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PomDependencyCollectorTest {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private PomDependencyCollector subject;

    private ArtifactMock artifact;

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    @Before
    public void setUpSubject() throws Exception {
        MavenContextImpl mavenContext = new MockMavenContext(new File("dummy"), logVerifier.getLogger());
        subject = new PomDependencyCollectorImpl(mavenContext, new ReactorProjectStub(tempManager.newFolder(), "test"));
    }

    @Test
    public void testAddArtifactWithExistingMetadataRespectsClassifier() throws Exception {
        // classifier matches one of the two IUs
        artifact = artifactWithClassifier("sources");

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = subject.getMavenInstallableUnits().keySet();
        assertThat(units, hasItem(unitWithId("test.unit.source")));
        assertEquals(1, units.size());
    }

    @Test
    public void testAddArtifactWithExistingMetadataOfMainArtifact() throws Exception {
        // main (i.e. null) classifier matches one of the two IUs
        artifact = artifactWithClassifier(null);

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = subject.getMavenInstallableUnits().keySet();
        assertThat(units, hasItem(unitWithId("test.unit")));
        assertEquals(1, units.size());
    }

    @Test
    public void testAddArtifactWithExistingMetadataButNonMatchingClassifier() throws Exception {
        // classifier does not match any of the available metadata
        artifact = artifactWithClassifier("classifier-not-in-p2-metadata");

        subject.addArtifactWithExistingMetadata(artifact, existingMetadata());

        Collection<IInstallableUnit> units = subject.getMavenInstallableUnits().keySet();
        assertTrue(units.isEmpty());
    }

    static ArtifactMock artifactWithClassifier(String classifier) throws Exception {
        return new ArtifactMock(
                new File("resources/platformbuilder/pom-dependencies/org.eclipse.osgi_3.5.2.R35x_v20100126.jar"),
                "groupId", "artifactId", "1", PackagingType.TYPE_ECLIPSE_PLUGIN, classifier);
    }

    static ArtifactMock existingMetadata() {
        return new ArtifactMock(new File("resources/platformbuilder/pom-dependencies/existing-p2-metadata.xml"),
                "groupId", "artifactId", "1", PackagingType.TYPE_ECLIPSE_PLUGIN, "p2metadata");
    }

}
