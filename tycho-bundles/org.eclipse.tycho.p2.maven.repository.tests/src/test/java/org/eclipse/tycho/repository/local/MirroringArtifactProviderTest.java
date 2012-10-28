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

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.RemoteArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.test.util.ProbeOutputStream;
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
    private static final Set<String> BUNDLE_B_CONTENT_FILES = TestRepositoryContent.BUNDLE_B_FILES;

    // bundle already in local repository
    private static final IArtifactKey BUNDLE_L_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.core.jobs",
            Version.parseVersion("3.4.1.R34x_v20081128"));
    private static final Set<String> BUNDLE_L_CONTENT_FILES = new HashSet<String>(Arrays.asList(new String[] {
            "META-INF/", "META-INF/MANIFEST.MF", "org/", "org/eclipse/", "org/eclipse/core/",
            "org/eclipse/core/internal/", "org/eclipse/core/internal/jobs/", "org/eclipse/core/runtime/",
            "org/eclipse/core/runtime/jobs/", "plugin.properties" }));

    @Rule
    public P2Context p2Context = new P2Context();

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private MirroringArtifactProvider subject;

    private File localRepositoryRoot;
    private LocalArtifactRepository localRepository;

    private ProbeOutputStream dataSink;

    @Before
    public void initSubject() throws Exception {
        // TODO extract methods
        IProvisioningAgent p2Agent = p2Context.getAgent();
        RepositoryArtifactProvider remoteProvider = new RepositoryArtifactProvider(
                Collections.singletonList(TestRepositoryContent.REPO_BUNDLE_AB), new RemoteArtifactTransferPolicy(),
                p2Agent);
        localRepositoryRoot = tempManager.newFolder("localRepo");
        FileUtils.copy(ResourceUtil.resourceFile("repositories/local_alt"), localRepositoryRoot, new File("."), true);
        LocalRepositoryP2Indices localRepoIndices = new LocalRepositoryP2IndicesImpl(localRepositoryRoot,
                new NoopFileLockService());
        localRepository = new LocalArtifactRepository(p2Agent, localRepoIndices);
        subject = new MirroringArtifactProvider(localRepository, remoteProvider);

        dataSink = new ProbeOutputStream();
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
    public void testGetAlreadyMirroredArtifact() throws Exception {
        assertMirrored(BUNDLE_L_KEY);

        subject.getArtifact(BUNDLE_L_KEY, dataSink, null);
        assertThat(dataSink.getFilesInZip(), is(BUNDLE_L_CONTENT_FILES));
    }

    @Test
    public void testGetArtifact() throws Exception {
        subject.getArtifact(BUNDLE_B_KEY, dataSink, null);
        assertThat(dataSink.getFilesInZip(), is(BUNDLE_B_CONTENT_FILES));

        assertMirrored(BUNDLE_B_KEY);
    }

    @Test
    public void testGetRawArtifact() throws Exception {
        // TODO use a packed artifact to ensure that this doesn't call getArtifact internally
        subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_B_KEY), dataSink, null);
        assertThat(dataSink.getFilesInZip(), is(BUNDLE_B_CONTENT_FILES));

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
