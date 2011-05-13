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
package org.eclipse.tycho.p2.resolver.impl;

import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasGAV;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasId;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasVersion;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.junit.Before;
import org.junit.Test;

public class ResolutionContextBundlePublisherTest {

    private static final String DUMMY_GROUP_ID = "org.eclipse.tycho.test.dummy";
    private static final String DUMMY_ARTIFACT_ID = "dummy-artifact";
    private static final String DUMMY_VERSION = "0.8.15-SNAPSHOT";

    private ResolutionContextBundlePublisher subject;

    @Before
    public void initSubject() {
        subject = new ResolutionContextBundlePublisher(new MavenLoggerStub(true));
    }

    @Test
    public void testPomDependencyOnBundle() throws Exception {
        File bundleFile = new File("resources/pom-dependencies/org.eclipse.osgi_3.5.2.R35x_v20100126.jar")
                .getCanonicalFile();
        IArtifactFacade bundleArtifact = new ArtifactMock(bundleFile, DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION,
                "jar");

        IInstallableUnit unit = subject.attemptToPublishBundle(bundleArtifact);

        assertThat(unit, hasId("org.eclipse.osgi"));
        assertThat(unit, hasVersion("3.5.2.R35x_v20100126"));
        assertThat(unit, hasGAV(DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION));

        // TODO check more properties of unit?
    }

    @Test
    public void testPomDependencyOnPlainJar() throws Exception {
        File jarFile = new File("resources/pom-dependencies/non-bundle.jar").getCanonicalFile();
        IArtifactFacade jarArtifact = new ArtifactMock(jarFile, DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION, "jar");

        IInstallableUnit unit = subject.attemptToPublishBundle(jarArtifact);

        assertNull(unit);
    }

    @Test
    public void testPomDependencyOnOtherType() throws Exception {
        File otherFile = new File("resources/pom-dependencies/other-type.xml").getCanonicalFile();
        IArtifactFacade otherArtifact = new ArtifactMock(otherFile, DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION,
                "pom");

        IInstallableUnit unit = subject.attemptToPublishBundle(otherArtifact);

        assertNull(unit);
    }
}
