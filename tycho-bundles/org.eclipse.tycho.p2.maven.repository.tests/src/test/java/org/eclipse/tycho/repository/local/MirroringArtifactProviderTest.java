/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.repository.general.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.general.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.repository.general.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.general.RemoteArtifactTransferPolicy;
import org.eclipse.tycho.repository.general.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.test.util.Md5DigestOutputStream;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class MirroringArtifactProviderTest {

    // remote bundles
    private static final IArtifactKey BUNDLE_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey BUNDLE_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;
    private static final String BUNDLE_B_CONTENT_MD5 = TestRepositoryContent.BUNDLE_B_CONTENT_MD5;

    // bundle already in local repository
    private static final IArtifactKey BUNDLE_L_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.core.jobs",
            Version.parseVersion("3.4.1.R34x_v20081128"));
    private static final String BUNDLE_L_CONTENT_MD5 = "638f5bcabe884f7d9f2999cbb9ae3c6";

    @Rule
    public P2Context p2Context = new P2Context();

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private MirroringArtifactProvider subject;

    private File localRepositoryRoot;
    private LocalArtifactRepository localRepository;

    private Md5DigestOutputStream dataSink;

    @Before
    public void initSubject() throws Exception {
        // TODO extract methods
        IProvisioningAgent p2Agent = p2Context.getAgent();
        RepositoryArtifactProvider remoteProvider = new RepositoryArtifactProvider(
                Collections.singletonList(TestRepositoryContent.REPO_BUNDLE_AB), new RemoteArtifactTransferPolicy(),
                p2Agent);
        localRepositoryRoot = tempManager.newFolder("localRepo");
        FileUtils.copy(ResourceUtil.resourceFile("repositories/local"), localRepositoryRoot, new File("."), true);
        LocalRepositoryP2Indices localRepoIndices = new LocalRepositoryP2IndicesImpl(localRepositoryRoot,
                new NoopFileLockService());
        localRepository = new LocalArtifactRepository(p2Agent, localRepoIndices);
        subject = new MirroringArtifactProvider(localRepository, remoteProvider);

        dataSink = new Md5DigestOutputStream();
    }

    @Test
    public void testContainsKey() {
        assertTrue(subject.contains(BUNDLE_A_KEY));
        assertTrue(subject.contains(BUNDLE_B_KEY));
        assertTrue(subject.contains(BUNDLE_L_KEY));

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
    public void testGetAlreadyMirroredArtifact() {
        assertMirrored(BUNDLE_L_KEY);

        subject.getArtifact(BUNDLE_L_KEY, dataSink, null);
        assertThat(dataSink.md5AsHex(), is(BUNDLE_L_CONTENT_MD5));
    }

    @Test
    public void testGetArtifact() {
        subject.getArtifact(BUNDLE_B_KEY, dataSink, null);
        assertThat(dataSink.md5AsHex(), is(BUNDLE_B_CONTENT_MD5));

        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetRawArtifact() {
        // TODO use a packed artifact to ensure that this doesn't call getArtifact internally
        subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_B_KEY), dataSink, null);
        assertThat(dataSink.md5AsHex(), is(BUNDLE_B_CONTENT_MD5));

        assertMirrored(BUNDLE_B_KEY);
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
    public void testGetRawArtifactFile() {
        assertThat(subject.getArtifactFile(canonicalDescriptorFor(BUNDLE_A_KEY)), is(new File(localRepositoryRoot,
                localRepoPathOf(BUNDLE_A_KEY))));

        assertMirrored(BUNDLE_A_KEY);
    }

    // TODO get*ArtifactFile null result case?

    @Test
    public void testGetArtifactDescriptors() {
        IArtifactDescriptor[] result = subject.getArtifactDescriptors(BUNDLE_B_KEY);

        assertThat(result.length, is(1));
        assertTrue(ArtifactTransferPolicy.isCanonicalFormat(result[0]));

        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testContainsArtifactDescriptor() {
        assertTrue(subject.contains(canonicalDescriptorFor(BUNDLE_A_KEY)));

        assertMirrored(BUNDLE_A_KEY);
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
