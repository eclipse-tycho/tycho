/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.hasGAV;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unit;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryMatchers.contains;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TargetPlatformBundlePublisherServiceTest {

    private static final String GROUP_ID = "example.group";
    private static final String ARTIFACT_ID = "example.artifact";
    private static final String VERSION = "0.8.15-SNAPSHOT";
    private static final String VERSION2 = "0.8.16-SNAPSHOT";

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    private File localRepositoryRoot;

    private TargetPlatformBundlePublisherService subject;

    @Before
    public void initSubject() {
        logVerifier.expectNoWarnings();

        localRepositoryRoot = tempFolder.getRoot();
        subject = new TargetPlatformBundlePublisherService(localRepositoryRoot, logVerifier.getLogger());
    }

    @Test
    public void testPomDependencyOnBundle() throws Exception {
        String bundleId = "org.eclipse.osgi";
        String bundleVersion = "3.5.2.R35x_v20100126";

        copyFolderContent(resourceFile("platformbuilder/pom-dependencies/bundle-repo"), localRepositoryRoot);
        File bundleFile = new File(localRepositoryRoot, RepositoryLayoutHelper.getRelativePath(GROUP_ID, ARTIFACT_ID,
                VERSION, null, "jar"));

        IArtifactFacade bundleArtifact = new ArtifactMock(bundleFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");

        IInstallableUnit publishedUnit = subject.attemptToPublishBundle(bundleArtifact);

        assertThat(publishedUnit, is(unit(bundleId, bundleVersion)));
        assertThat(publishedUnit, hasGAV(GROUP_ID, ARTIFACT_ID, VERSION));
        assertThat(publishedUnit.getArtifacts().size(), is(1));

        IArtifactKey referencedArtifact = publishedUnit.getArtifacts().iterator().next();
        IRawArtifactProvider artifactRepo = subject.getArtifactRepoOfPublishedBundles();
        assertThat(artifactRepo, contains(referencedArtifact));

        // test that reading the artifact succeeds (because the way it is added to the repository is a bit special) 
        assertThat(artifactMD5Of(referencedArtifact, artifactRepo), is("6303323acc98658c0fed307c84db4411"));
    }

    @Test
    public void testPomDependencyOnPlainJar() throws Exception {
        File jarFile = resourceFile("platformbuilder/pom-dependencies/non-bundle.jar");
        IArtifactFacade jarArtifact = new ArtifactMock(jarFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");

        IInstallableUnit unit = subject.attemptToPublishBundle(jarArtifact);

        assertNull(unit);
    }

    @Test
    public void testPomDependencyOnOtherType() throws Exception {
        File otherFile = resourceFile("platformbuilder/pom-dependencies/other-type.xml");
        IArtifactFacade otherArtifact = new ArtifactMock(otherFile, GROUP_ID, ARTIFACT_ID, VERSION, "pom");

        IInstallableUnit unit = subject.attemptToPublishBundle(otherArtifact);

        assertNull(unit);
    }

    @Test
    public void testCaching() throws Exception {
        copyFolderContent(resourceFile("platformbuilder/pom-dependencies/bundle-repo"), localRepositoryRoot);
        File bundleFile = new File(localRepositoryRoot, RepositoryLayoutHelper.getRelativePath(GROUP_ID, ARTIFACT_ID,
                VERSION, null, "jar"));
        File bundleFile2 = new File(localRepositoryRoot, RepositoryLayoutHelper.getRelativePath(GROUP_ID, ARTIFACT_ID,
                VERSION2, null, "jar"));
        IArtifactFacade bundleArtifact = new ArtifactMock(bundleFile, GROUP_ID, ARTIFACT_ID, VERSION, "jar");
        IArtifactFacade bundleArtifact2 = new ArtifactMock(bundleFile2, GROUP_ID, ARTIFACT_ID, VERSION2, "jar");

        IInstallableUnit unit1 = subject.attemptToPublishBundle(bundleArtifact);
        IInstallableUnit unit2 = subject.attemptToPublishBundle(bundleArtifact2);
        IInstallableUnit unit1SecondTime = subject.attemptToPublishBundle(bundleArtifact);
        assertTrue(unit1 == unit1SecondTime);
        assertTrue(!unit1.equals(unit2));
        assertTrue(!unit1SecondTime.equals(unit2));
    }

    private static String artifactMD5Of(IArtifactKey key, IRawArtifactProvider artifactProvider) throws Exception {
        ProbeRawArtifactSink probeSink = ProbeRawArtifactSink.newRawArtifactSinkFor(new ArtifactDescriptor(key));
        artifactProvider.getArtifact(probeSink, null);
        return probeSink.md5AsHex();
    }

    @SuppressWarnings("restriction")
    private static void copyFolderContent(File sourceFolder, File targetFolder) throws IOException {
        FileUtils.copy(sourceFolder, targetFolder, new File("."), true);
    }

}
