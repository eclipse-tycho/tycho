/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.local;

import static java.util.Collections.singletonList;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.local.MirroringArtifactProvider.MirroringFailedException;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MirroringArtifactProviderErrorTest {

    private static final IArtifactKey CORRUPT_ARTIFACT = TestRepositoryContent.BUNDLE_B_KEY;
    private static final IArtifactKey ARTIFACT_ONLY_PACK200_CORRUPT = TestRepositoryContent.BUNDLE_A_KEY;

    @Rule
    public LogVerifier logVerifier = new LogVerifier();
    @Rule
    public P2Context p2Context = new P2Context();
    @Rule
    public TemporaryLocalMavenRepository tempLocalMavenRepository = new TemporaryLocalMavenRepository();

    private LocalArtifactRepository localRepository;
    private ProbeArtifactSink testSink;

    MirroringArtifactProvider subject;

    @Before
    public void before() throws Exception {
        localRepository = tempLocalMavenRepository.getLocalArtifactRepository();

        subject = MirroringArtifactProvider.createInstance(localRepository,
                new RepositoryArtifactProvider(singletonList(TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT),
                        ArtifactTransferPolicies.forRemoteArtifacts(), p2Context.getAgent()),
                false, logVerifier.getLogger());
    }

    @Test(expected = MirroringFailedException.class)
    public void testMirrorCorruptArtifact() throws Exception {
        logVerifier.expectError(CORRUPT_ARTIFACT.toString());

        testSink = newArtifactSinkFor(CORRUPT_ARTIFACT);
        try {
            // here we expect an exception, an not (!) an error status, to be consistent with other methods that mirror but don't return a status
            subject.getArtifact(testSink, null);

        } finally {
            assertNotMirrored(CORRUPT_ARTIFACT);
            assertThat(testSink.writeIsStarted(), is(false));
        }
    }

    @Test
    public void testMirrorArtifactWhichOnlySucceedsInSecondAttempt() throws Exception {
        Assume.assumeTrue("This test requires pack200", Runtime.version().feature() < 14);
        assertThat(subject.contains(canonicalDescriptorFor(ARTIFACT_ONLY_PACK200_CORRUPT)), is(true));

        assertMirrored(ARTIFACT_ONLY_PACK200_CORRUPT);

        logVerifier.expectWarning("Some attempts to read artifact " + ARTIFACT_ONLY_PACK200_CORRUPT + " failed");
    }

    private void assertNotMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, is(0));
    }

    private void assertMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, not(is(0)));
    }
}
