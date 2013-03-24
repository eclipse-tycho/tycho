/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.packedDescriptorFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ProbeArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ProbeRawArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class MirroringArtifactProviderTest {

    // remote bundles
    private static final IArtifactKey BUNDLE_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey BUNDLE_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;
    private static final Set<String> BUNDLE_B_FILES = TestRepositoryContent.BUNDLE_B_FILES;

    // bundle already in local repository
    private static final IArtifactKey BUNDLE_L_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.core.jobs",
            Version.parseVersion("3.4.1.R34x_v20081128"));
    private static final Set<String> BUNDLE_L_CONTENT_FILES = new HashSet<String>(Arrays.asList(new String[] {
            "META-INF/", "META-INF/MANIFEST.MF", "org/", "org/eclipse/", "org/eclipse/core/",
            "org/eclipse/core/internal/", "org/eclipse/core/internal/jobs/", "org/eclipse/core/runtime/",
            "org/eclipse/core/runtime/jobs/", "plugin.properties" }));

    // not available bundle
    private static final IArtifactKey OTHER_KEY = TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();
    @Rule
    public TemporaryLocalMavenRepository localRepositoryManager = new TemporaryLocalMavenRepository();
    private File localRepositoryRoot;
    private LocalArtifactRepository localRepository;

    private ProbeArtifactSink testSink;
    private ProbeRawArtifactSink rawTestSink;

    private MirroringArtifactProvider subject;
    private IStatus status;

    @Before
    public void initSubject() throws Exception {
        RepositoryArtifactProvider remoteProvider = new RepositoryArtifactProvider(
                Collections.singletonList(TestRepositoryContent.REPO_BUNDLE_AB),
                ArtifactTransferPolicies.forRemoteArtifacts(), p2Context.getAgent());

        localRepositoryRoot = localRepositoryManager.getLocalRepositoryRoot();
        FileUtils.copy(ResourceUtil.resourceFile("repositories/local_alt"), localRepositoryRoot, new File("."), true);
        localRepository = localRepositoryManager.getLocalArtifactRepository();

        subject = new MirroringArtifactProvider(localRepository, remoteProvider);
    }

    @After
    public void checkStatusAndSinkConsistency() {
        if (testSink != null) {
            testSink.checkConsistencyWithStatus(status);
        }
        if (rawTestSink != null) {
            rawTestSink.checkConsistencyWithStatus(status);
        }
    }

    @Test
    public void testContainsKey() {
        assertTrue(subject.contains(BUNDLE_A_KEY));
        assertTrue(subject.contains(BUNDLE_B_KEY));
        assertTrue(subject.contains(BUNDLE_L_KEY));
        assertFalse(subject.contains(OTHER_KEY));

        assertFalse(subject.contains(new ArtifactKey(BUNDLE_A_KEY.getClassifier(), BUNDLE_A_KEY.getId(),
                Version.emptyVersion)));

        // no download triggered
        assertNotMirrored(BUNDLE_A_KEY);
        assertNotMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testQuery() {
        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertThat(result.toSet().size(), is(3));

        assertNotMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetAlreadyMirroredArtifact() throws Exception {
        assertMirrored(BUNDLE_L_KEY);

        testSink = newArtifactSinkFor(BUNDLE_L_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, okStatus());
        assertThat(testSink.getFilesInZip(), is(BUNDLE_L_CONTENT_FILES));
    }

    @Test
    public void testGetArtifact() throws Exception {
        testSink = newArtifactSinkFor(BUNDLE_B_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.getFilesInZip(), is(BUNDLE_B_FILES));

        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetUnavailableArtifact() throws Exception {
        testSink = newArtifactSinkFor(OTHER_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testGetAlreadyMirroredArtifactFile() {
        assertThat(subject.getArtifactFile(BUNDLE_L_KEY), is(new File(localRepositoryRoot,
                localRepoPathOf(BUNDLE_L_KEY))));
    }

    @Test
    public void testGetArtifactFile() {
        assertThat(subject.getArtifactFile(BUNDLE_A_KEY), is(new File(localRepositoryRoot,
                localRepoPathOf(BUNDLE_A_KEY))));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetUnavailableArtifactFile() {
        assertThat(subject.getArtifactFile(OTHER_KEY), is(nullValue()));
    }

    @Test
    public void testGetArtifactDescriptors() {
        IArtifactDescriptor[] result = subject.getArtifactDescriptors(BUNDLE_B_KEY);

        assertThat(result.length, is(1));
        assertTrue(ArtifactTransferPolicy.isCanonicalFormat(result[0]));

        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetArtifactDescriptorsOfUnavailableArtifact() {
        assertThat(subject.getArtifactDescriptors(OTHER_KEY).length, is(0));
    }

    @Test
    public void testContainsCanonicalArtifactDescriptor() {
        assertTrue(subject.contains(canonicalDescriptorFor(BUNDLE_A_KEY)));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testContainsPackedArtifactDescriptor() {
        assertFalse(subject.contains(packedDescriptorFor(BUNDLE_A_KEY)));

        // any descriptor access triggers the mirroring
        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testContainsArtifactDescriptorOfUnavailableArtifact() {
        assertFalse(subject.contains(canonicalDescriptorFor(OTHER_KEY)));
    }

    @Test
    public void testGetRawCanonicalArtifactFile() {
        // method that takes a descriptor returns the raw file
        assertThat(subject.getArtifactFile(canonicalDescriptorFor(BUNDLE_A_KEY)), is(new File(localRepositoryRoot,
                localRepoPathOf(BUNDLE_A_KEY))));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetRawArtifactFileOfUnavailableFile() {
        assertThat(subject.getArtifactFile(canonicalDescriptorFor(OTHER_KEY)), is(nullValue()));
    }

    @Test
    public void testGetRawCanonicalArtifact() throws Exception {
        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_B_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.getFilesInZip(), is(BUNDLE_B_FILES));

        // any descriptor access triggers the mirroring
        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetRawPackedArtifactFails() throws Exception {
        rawTestSink = newRawArtifactSinkFor(packedDescriptorFor(BUNDLE_B_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));

        // the default MirroringArtifactProvider doesn't mirror packed artifacts
        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetRawArtifactOfUnavailableArtifactFails() throws Exception {
        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(OTHER_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    private void assertNotMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, is(0));
    }

    private void assertMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, not(is(0)));
    }

    private static String localRepoPathOf(IArtifactKey key) {
        return "p2/" + key.getClassifier().replace('.', '/') + "/" + key.getId() + "/" + key.getVersion() + "/"
                + key.getId() + "-" + key.getVersion() + ".jar";
    }

}
