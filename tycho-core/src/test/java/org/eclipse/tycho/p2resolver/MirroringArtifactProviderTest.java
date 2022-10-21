/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.test.util.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.MirroringArtifactProvider;
import org.eclipse.tycho.p2.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.test.util.ProbeArtifactSink;
import org.eclipse.tycho.test.util.ProbeRawArtifactSink;
import org.eclipse.tycho.test.util.TemporaryLocalMavenRepository;
import org.eclipse.tycho.test.util.TestRepositoryContent;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("restriction")
public class MirroringArtifactProviderTest extends TychoPlexusTestCase {

    // remote bundles
    private static final IArtifactKey BUNDLE_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey BUNDLE_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;

    // bundle already in local repository
    private static final IArtifactKey BUNDLE_L_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.core.jobs",
            Version.parseVersion("3.4.1.R34x_v20081128"));
    private static final Set<String> BUNDLE_L_CONTENT_FILES = new HashSet<>(
            Arrays.asList("META-INF/", "META-INF/MANIFEST.MF", "org/", "org/eclipse/", "org/eclipse/core/",
                    "org/eclipse/core/internal/", "org/eclipse/core/internal/jobs/", "org/eclipse/core/runtime/",
                    "org/eclipse/core/runtime/jobs/", "plugin.properties"));

    // not available bundle
    private static final IArtifactKey OTHER_KEY = TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

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
                ArtifactTransferPolicies.forLocalArtifacts(), lookup(IProvisioningAgent.class));

        // initialize local repository content (see BUNDLE_L_KEY)
        localRepositoryRoot = localRepositoryManager.getLocalRepositoryRoot();
        FileUtils.copy(ResourceUtil.resourceFile("repositories/local_alt"), localRepositoryRoot, new File("."), true);
        localRepository = localRepositoryManager.getLocalArtifactRepository();

        subject = MirroringArtifactProvider.createInstance(localRepository, remoteProvider,
                new MockMavenContext(null, logVerifier.getLogger()));
    }

    @Before
    public void expectNoWarningsInLog() throws Exception {
        logVerifier.expectNoWarnings();
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

        assertFalse(subject
                .contains(new ArtifactKey(BUNDLE_A_KEY.getClassifier(), BUNDLE_A_KEY.getId(), Version.emptyVersion)));

        // no download triggered
        assertNotMirrored(BUNDLE_A_KEY);
        assertNotMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testQuery() {
        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertEquals(3, result.toSet().size());

        assertNotMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetAlreadyMirroredArtifact() throws Exception {
        assertMirrored(BUNDLE_L_KEY);

        testSink = newArtifactSinkFor(BUNDLE_L_KEY);
        status = subject.getArtifact(testSink, null);

        assertTrue(status.isOK());
        assertEquals(BUNDLE_L_CONTENT_FILES, testSink.getFilesInZip());
    }

    @Test
    public void testGetUnavailableArtifact() throws Exception {
        testSink = newArtifactSinkFor(OTHER_KEY);
        status = subject.getArtifact(testSink, null);

        assertFalse(testSink.writeIsStarted());
        assertEquals(IStatus.ERROR, status.getSeverity());
        assertEquals(ProvisionException.ARTIFACT_NOT_FOUND, status.getCode());
    }

    @Test
    public void testGetAlreadyMirroredArtifactFile() {
        assertEquals(new File(localRepositoryRoot, localRepoPathOf(BUNDLE_L_KEY)),
                subject.getArtifactFile(BUNDLE_L_KEY));
    }

    @Test
    public void testGetArtifactFile() {
        assertEquals(new File(localRepositoryRoot, localRepoPathOf(BUNDLE_A_KEY)),
                subject.getArtifactFile(BUNDLE_A_KEY));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetUnavailableArtifactFile() {
        assertNull(subject.getArtifactFile(OTHER_KEY));
    }

    @Test
    public void testGetArtifactDescriptorsOfUnavailableArtifact() {
        assertEquals(0, subject.getArtifactDescriptors(OTHER_KEY).length);
    }

    @Test
    public void testContainsCanonicalArtifactDescriptor() {
        assertTrue(subject.contains(canonicalDescriptorFor(BUNDLE_A_KEY)));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testContainsArtifactDescriptorOfUnavailableArtifact() {
        assertFalse(subject.contains(canonicalDescriptorFor(OTHER_KEY)));
    }

    @Test
    public void testGetRawCanonicalArtifactFile() {
        // the getArtifactFile method that takes a descriptor returns the raw file
        assertEquals(new File(localRepositoryRoot, localRepoPathOf(BUNDLE_A_KEY)),
                subject.getArtifactFile(canonicalDescriptorFor(BUNDLE_A_KEY)));

        assertMirrored(BUNDLE_A_KEY);
    }

    @Test
    public void testGetRawArtifactFileOfUnavailableFile() {
        assertNull(subject.getArtifactFile(canonicalDescriptorFor(OTHER_KEY)));
    }

    @Test
    public void testGetRawArtifactOfUnavailableArtifactFails() throws Exception {
        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(OTHER_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertFalse(rawTestSink.writeIsStarted());
        assertEquals(IStatus.ERROR, status.getSeverity());
        assertEquals(ProvisionException.ARTIFACT_NOT_FOUND, status.getCode());
    }

    private void assertNotMirrored(IArtifactKey key) {
        assertEquals(0, localRepository.getArtifactDescriptors(key).length);
    }

    private void assertMirrored(IArtifactKey key) {
        assertNotEquals(0, localRepository.getArtifactDescriptors(key).length);
    }

    private static String localRepoPathOf(IArtifactKey key) {
        return localRepoPathOf(key, ".jar");
    }

    private static String localRepoPathOf(IArtifactKey key, String classifierAndExtension) {
        return "p2/" + key.getClassifier().replace('.', '/') + "/" + key.getId() + "/" + key.getVersion() + "/"
                + key.getId() + "-" + key.getVersion() + classifierAndExtension;
    }

}
