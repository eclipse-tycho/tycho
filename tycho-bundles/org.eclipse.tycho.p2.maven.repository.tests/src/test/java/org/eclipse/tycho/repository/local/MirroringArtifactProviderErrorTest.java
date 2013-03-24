package org.eclipse.tycho.repository.local;

import static java.util.Collections.singletonList;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ProbeArtifactSink.newArtifactSinkFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.local.MirroringArtifactProvider.MirroringFailedException;
import org.eclipse.tycho.repository.p2base.artifact.provider.ProbeArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.RemoteArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MirroringArtifactProviderErrorTest {

    private static final IArtifactKey CORRUPT_ARTIFACT = TestRepositoryContent.BUNDLE_B_KEY;
    private static final IArtifactKey ARTIFACT_ONLY_PACK200_CORRUPT = TestRepositoryContent.BUNDLE_A_KEY;

    @Rule
    public TemporaryLocalMavenRepository tempLocalMavenRepository = new TemporaryLocalMavenRepository();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public P2Context p2Context = new P2Context();

    private LocalArtifactRepository localRepository;
    private ProbeArtifactSink testSink;

    MirroringArtifactProvider subject;

    @Before
    public void before() throws Exception {
        localRepository = tempLocalMavenRepository.getLocalArtifactRepository();

        subject = new MirroringArtifactProvider(localRepository, new RepositoryArtifactProvider(
                singletonList(TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT), new RemoteArtifactTransferPolicy(),
                p2Context.getAgent()));
    }

    @Test
    public void testMirrorCorruptArtifact() throws Exception {
        // here we expect an exception, an not (!) an error status, to be consistent with other methods that mirror but don't return a status
        expectedException.expect(MirroringFailedException.class);
        try {
            testSink = newArtifactSinkFor(CORRUPT_ARTIFACT);
            subject.getArtifact(testSink, null);

        } finally {
            assertNotMirrored(CORRUPT_ARTIFACT);
            assertThat(testSink.writeIsStarted(), is(false));
        }

    }

    @Test
    public void testMirrorArtifactWhichOnlySucceedsInSecondAttempt() throws Exception {
        assertThat(subject.contains(canonicalDescriptorFor(ARTIFACT_ONLY_PACK200_CORRUPT)), is(true));

        assertMirrored(ARTIFACT_ONLY_PACK200_CORRUPT);
        // TODO assert that a warning is logged
    }

    private void assertNotMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, is(0));
    }

    private void assertMirrored(IArtifactKey key) {
        assertThat(localRepository.getArtifactDescriptors(key).length, not(is(0)));
    }
}
