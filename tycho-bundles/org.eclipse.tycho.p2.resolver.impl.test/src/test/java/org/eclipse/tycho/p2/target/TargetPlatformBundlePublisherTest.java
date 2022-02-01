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
 *    Christoph Läubrich -  Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *                          Bug 567639 - wrapAsBundle fails when dealing with esoteric versions
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unit;
import static org.eclipse.tycho.repository.testutil.ArtifactPropertiesMatchers.containsGAV;
import static org.eclipse.tycho.repository.testutil.ArtifactPropertiesMatchers.hasProperty;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryMatchers.contains;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.resolver.WrappedArtifact;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import aQute.bnd.version.Version;

public class TargetPlatformBundlePublisherTest {

    private static final String GROUP_ID = "example.group";
    private static final String ARTIFACT_ID = "example.artifact";
    private static final String VERSION = "0.8.15-SNAPSHOT";

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    private File localRepositoryRoot;

    private TargetPlatformBundlePublisher subject;

    @Before
    public void initSubject() throws IOException {

        localRepositoryRoot = tempFolder.getRoot();
        MockMavenContext mavenContext = new MockMavenContext(localRepositoryRoot, false, logVerifier.getLogger(),
                new Properties());
        subject = new TargetPlatformBundlePublisher(new ReactorProjectStub(tempFolder.newFolder(), "test"),
                mavenContext);
    }

    @Test
    public void testPomDependencyOnBundle() throws Exception {
        logVerifier.expectNoWarnings();
        String bundleId = "org.eclipse.osgi";
        String bundleVersion = "3.5.2.R35x_v20100126";

        FileUtils.copyDirectory(resourceFile("platformbuilder/pom-dependencies/bundle-repo"), localRepositoryRoot);
        File bundleFile = new File(localRepositoryRoot,
                RepositoryLayoutHelper.getRelativePath(GROUP_ID, ARTIFACT_ID, VERSION, null, "jar"));
        IArtifactFacade bundleArtifact = new ArtifactMock(bundleFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");

        IInstallableUnit publishedUnit = subject.attemptToPublishBundle(bundleArtifact, false).getUnit();

        assertThat(publishedUnit, is(unit(bundleId, bundleVersion)));
        assertThat(publishedUnit.getProperties(), containsGAV(GROUP_ID, ARTIFACT_ID, VERSION));
        assertThat(publishedUnit.getArtifacts().size(), is(1));

        IArtifactKey referencedArtifact = publishedUnit.getArtifacts().iterator().next();
        IRawArtifactProvider artifactRepo = subject.getArtifactRepoOfPublishedBundles();
        assertThat(artifactRepo, contains(referencedArtifact));

        IArtifactDescriptor[] artifactDescriptors = artifactRepo.getArtifactDescriptors(referencedArtifact);
        assertThat(artifactDescriptors.length, is(1));
        assertThat(artifactDescriptors[0].getProperties(), containsGAV(GROUP_ID, ARTIFACT_ID, VERSION));
        assertThat(artifactDescriptors[0].getProperties(),
                hasProperty("download.md5", "6303323acc98658c0fed307c84db4411"));

        // test that reading the artifact succeeds (because the way it is added to the repository is a bit special) 
        assertThat(artifactMD5Of(referencedArtifact, artifactRepo), is("6303323acc98658c0fed307c84db4411"));
    }

    @Test
    public void testPomDependencyOnPlainJar() throws Exception {
        logVerifier.expectNoWarnings();
        logVerifier.expectInfo(containsString(
                "is not a bundle and will be ignored, automatic wrapping of such artifacts can be enabled with <pomDependencies>wrapAsBundle</pomDependencies>"));
        File jarFile = resourceFile("platformbuilder/pom-dependencies/non-bundle.jar");
        IArtifactFacade jarArtifact = new ArtifactMock(jarFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");
        assertNull(subject.attemptToPublishBundle(jarArtifact, false));
    }

    @Test
    public void testPomDependencyWrapPlainJar() throws Exception {
        logVerifier.expectWarning(
                containsString("is not a bundle and was automatically wrapped with bundle-symbolic name"));
        File jarFile = resourceFile("platformbuilder/pom-dependencies/non-bundle.jar");
        IArtifactFacade jarArtifact = new ArtifactMock(jarFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");
        MavenBundleInfo publishBundle = subject.attemptToPublishBundle(jarArtifact, true);
        assertNotNull(publishBundle);
    }

    @Test
    public void testPomDependencyOnOtherType() throws Exception {
        logVerifier.expectNoWarnings();
        File otherFile = resourceFile("platformbuilder/pom-dependencies/other-type.xml");
        IArtifactFacade otherArtifact = new ArtifactMock(otherFile, GROUP_ID, ARTIFACT_ID, VERSION, "pom");
        assertNull(subject.attemptToPublishBundle(otherArtifact, false));
    }

    @Test
    public void testVersions() {
        assertEquals(new Version(0, 0, 1, "RELEASE121"),
                WrappedArtifact.createOSGiVersionFromArtifact(new ArtifactMock(localRepositoryRoot, "org.netbeans.api",
                        "org-netbeans-api-annotations-common", "RELEASE121", "jar")));
        assertEquals(new Version(1, 0, 0, "SNAPSHOT"), WrappedArtifact.createOSGiVersionFromArtifact(
                new ArtifactMock(localRepositoryRoot, "test", "me", "1.0.0-SNAPSHOT", "jar")));
        assertEquals(new Version(2, 2, 0, "Final"), WrappedArtifact.createOSGiVersionFromArtifact(
                new ArtifactMock(localRepositoryRoot, "io.undertow", "undertow-core", "2.2.0.Final", "jar")));
    }

    private static String artifactMD5Of(IArtifactKey key, IRawArtifactProvider artifactProvider) throws Exception {
        ProbeRawArtifactSink probeSink = ProbeRawArtifactSink.newRawArtifactSinkFor(new ArtifactDescriptor(key));
        artifactProvider.getArtifact(probeSink, null);
        return probeSink.md5AsHex();
    }

}
