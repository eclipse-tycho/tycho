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

import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.eclipse.tycho.p2.test.matcher.ArtifactRepositoryMatcher.contains;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasGAV;
import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.unit;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
    public void initSubject() {
        logVerifier.expectNoWarnings();

        localRepositoryRoot = tempFolder.getRoot();
        subject = new TargetPlatformBundlePublisher(localRepositoryRoot, logVerifier.getLogger());
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

    // TODO 368596 also test with classifier

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
