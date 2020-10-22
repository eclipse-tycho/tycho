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
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy.isCanonicalFormat;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.packedDescriptorFor;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.local.MirroringArtifactProvider.MirroringFailedException;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class MirroringArtifactProviderPack200CornerCasesTest {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forRemoteArtifacts();

    @Rule
    public LogVerifier logVerifier = new LogVerifier();
    @Rule
    public P2Context p2Context = new P2Context();

    @Rule
    public TemporaryLocalMavenRepository localRepositoryManager = new TemporaryLocalMavenRepository();
    private LocalArtifactRepository localRepository;

    private MirroringArtifactProvider subject;

    @Before
    public void initFields() throws Exception {
        localRepository = localRepositoryManager.getLocalArtifactRepository();
    }

    @Before
    public void expectNoWarningsInLog() throws Exception {
        logVerifier.expectNoWarnings();
    }

    @Test
    public void testIgnoreIfPackedArtifactNotAvailableRemotely() throws Exception {
        subject = MirroringArtifactProvider.createInstance(localRepository,
                providerFor(TestRepositoryContent.REPO_BUNDLE_A), true, logVerifier.getLogger());

        IArtifactDescriptor[] mirroredDescriptors = subject.getArtifactDescriptors(BUNDLE_A_KEY);

        assertThat(mirroredDescriptors.length, is(1));
        assertThat(isCanonicalFormat(mirroredDescriptors[0]), is(true));
    }

    @Test
    public void testErrorIfPackedArtifactIsAvailableButCorrupt() throws Exception {
        subject = MirroringArtifactProvider.createInstance(localRepository,
                providerFor(TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT), true, logVerifier.getLogger());

        logVerifier.expectError(containsString(BUNDLE_A_KEY.toString()));

        MirroringFailedException e = assertThrows(MirroringFailedException.class,
                () -> subject.getArtifactDescriptors(BUNDLE_A_KEY));
        assertTrue(e.getMessage().contains(BUNDLE_A_KEY.toString()));
    }

    @Test
    public void testPackedArtifactMirroredEvenIfCanonicalArtifactPresent() throws Exception {
        IRawArtifactProvider remoteProvider = providerFor(TestRepositoryContent.REPO_BUNDLE_AB);

        prefillLocalRepositoryWithCanonicalArtifact(localRepository, remoteProvider, BUNDLE_A_KEY);
        assertThat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY).length, is(1)); // self-test
        assertThat(isCanonicalFormat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY)[0]), is(true)); // self-test

        subject = MirroringArtifactProvider.createInstance(localRepository, remoteProvider, true,
                logVerifier.getLogger());

        assertThat(subject.getArtifactDescriptors(BUNDLE_A_KEY).length, is(2));
        assertThat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY).length, is(2));
    }

    @DataPoints
    public static Boolean mirrorPacked[] = new Boolean[] { true, false };

    @Theory
    public void testCanonicalArtifactCreatedIfPackedArtifactAlreadyMirrored(Boolean mirrorPacked) throws Exception {
        if (Runtime.version().feature() >= 14) {
            // This test doesn't make sense in non-pack200 friendly envs
            // we cannot easily use Assume with Theory
            return;
        }
        prefillLocalRepositoryWithPackedArtifact(localRepository, providerFor(TestRepositoryContent.REPO_BUNDLE_AB),
                BUNDLE_A_KEY);
        assertThat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY).length, is(1)); // self-test
        assertThat(isCanonicalFormat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY)[0]), is(false)); // self-test

        // expect no remote download
        RepositoryArtifactProvider emptyProvider = providerFor();

        // also expect this for the non-pack200 mirroring implementation (so that it doesn't fail if a different build left the local Maven repository in this state)
        subject = MirroringArtifactProvider.createInstance(localRepository, emptyProvider, mirrorPacked,
                logVerifier.getLogger());

        assertThat(subject.getArtifactDescriptors(BUNDLE_A_KEY).length, is(2));
        assertThat(localRepository.getArtifactDescriptors(BUNDLE_A_KEY).length, is(2));
    }

    private RepositoryArtifactProvider providerFor(URI... artifactRepository) throws ProvisionException {
        return new RepositoryArtifactProvider(Arrays.asList(artifactRepository), TRANSFER_POLICY, p2Context.getAgent());
    }

    private static void prefillLocalRepositoryWithPackedArtifact(LocalArtifactRepository localRepository,
            IRawArtifactProvider provider, IArtifactKey artifactKey) throws Exception {
        provider.getRawArtifact(localRepository.newAddingRawArtifactSink(packedDescriptorFor(artifactKey)), null);
    }

    private static void prefillLocalRepositoryWithCanonicalArtifact(LocalArtifactRepository localRepository,
            IRawArtifactProvider provider, IArtifactKey artifactKey) throws Exception {
        provider.getArtifact(localRepository.newAddingArtifactSink(artifactKey), null);
    }

}
