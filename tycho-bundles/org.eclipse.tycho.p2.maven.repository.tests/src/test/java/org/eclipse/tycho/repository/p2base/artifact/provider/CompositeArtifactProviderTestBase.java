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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_CONTENT_FILES;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_CONTENT_FILES;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_PACKED_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.inCanonicalFormat;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.inPackedFormat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.test.util.ProbeOutputStream;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("restriction")
public abstract class CompositeArtifactProviderTestBase<T extends IRawArtifactProvider> {

    @Rule
    public P2Context p2Context = new P2Context();

    public T subject;

    public ProbeOutputStream testSink;

    public abstract T createCompositeArtifactProvider(URI... repositoryURLs) throws Exception;

    @Before
    public void initContextAndSubject() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A, REPO_BUNDLE_AB);

        testSink = new ProbeOutputStream();
    }

    @After
    public void closeStream() throws Exception {
        testSink.close();
    }

    @Test
    public void testContainsKey() {
        assertTrue(subject.contains(BUNDLE_A_KEY));
        assertTrue(subject.contains(BUNDLE_B_KEY));

        String otherClassifier = "org.eclipse.update.feature";
        Version otherVersion = Version.emptyVersion;
        assertFalse(subject.contains(new ArtifactKey(otherClassifier, BUNDLE_A_KEY.getId(), BUNDLE_A_KEY.getVersion())));
        assertFalse(subject.contains(new ArtifactKey(BUNDLE_A_KEY.getClassifier(), BUNDLE_A_KEY.getId(), otherVersion)));
    }

    @Test
    public void testQuery() {
        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertThat(result.toSet().size(), is(2));
    }

    @Test
    public void testQueryWithSingleRepository() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A);

        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertThat(result.toSet().size(), is(1));
    }

    @Test
    public void testQueryWithoutRepositories() throws Exception {
        subject = createCompositeArtifactProvider(new URI[0]);

        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertThat(result.toSet().size(), is(0));
    }

    @Test
    public void testGetArtifact() throws Exception {
        subject.getArtifact(BUNDLE_A_KEY, testSink, null);
        assertThat(testSink.getFilesInZip(), is(BUNDLE_A_CONTENT_FILES));
    }

    @Test
    public void testGetArtifactOnlyAvailableInPackedRawFormat() throws Exception {
        subject.getArtifact(BUNDLE_B_KEY, testSink, null);
        assertThat(testSink.getFilesInZip(), is(BUNDLE_B_CONTENT_FILES));
    }

    @Test
    public void testGetArtifactDescriptors() {
        List<IArtifactDescriptor> descriptors = Arrays.asList(subject.getArtifactDescriptors(BUNDLE_B_KEY));

        assertThat(descriptors, hasItem(inPackedFormat()));
        assertThat(descriptors.size(), is(1));
    }

    @Test
    public void testGetArtifactDescriptorsDoesNotReturnDuplicates() {
        // the two source repositories each contain bundle A in canonical form
        List<IArtifactDescriptor> result = Arrays.asList(subject.getArtifactDescriptors(BUNDLE_A_KEY));

        assertThat(result, hasItem(inCanonicalFormat()));
        assertThat(result, hasItem(inPackedFormat()));
        assertThat(result.size(), is(2)); // no duplicates
    }

    @Test
    public void testContainsArtifactDescriptor() {
        assertThat(subject.contains(canonicalDescriptorFor(BUNDLE_A_KEY)), is(true));
        assertThat(subject.contains(canonicalDescriptorFor(BUNDLE_B_KEY)), is(false));
    }

    @Test
    public void testGetCanonicalRawArtifact() throws Exception {
        subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testSink, null);
        assertThat(testSink.md5AsHex(), is(BUNDLE_A_CONTENT_MD5)); // raw artifacts are never processed -> files should be identical
    }

    @Test
    public void testGetPackedRawArtifact() throws Exception {
        IArtifactDescriptor packedBundleB = subject.getArtifactDescriptors(BUNDLE_B_KEY)[0];
        subject.getRawArtifact(packedBundleB, testSink, null);
        assertThat(testSink.md5AsHex(), is(BUNDLE_B_PACKED_MD5));
    }

}
