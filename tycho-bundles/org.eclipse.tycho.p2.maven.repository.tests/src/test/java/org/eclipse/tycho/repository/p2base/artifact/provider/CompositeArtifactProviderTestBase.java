/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_FILES;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_PACKED_CONTENT_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_FILES;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_PACKED_CONTENT_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.inCanonicalFormat;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.inPackedFormat;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.packedDescriptorFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.statusWithMessageWhich;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.local.NonStartableArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkFactory;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;
import org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink;
import org.eclipse.tycho.repository.streaming.testutil.ProbeOutputStream;
import org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("restriction")
public abstract class CompositeArtifactProviderTestBase<T extends IRawArtifactProvider> {

    @Rule
    public P2Context p2Context = new P2Context();

    protected ProbeArtifactSink testSink;
    protected ProbeRawArtifactSink rawTestSink;
    protected ProbeOutputStream testOutputStream = new ProbeOutputStream();

    protected T subject;
    protected IStatus status;

    protected abstract T createCompositeArtifactProvider(URI... repositoryURLs) throws Exception;

    @Before
    public void initContextAndSubject() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A, REPO_BUNDLE_AB);
    }

    @After
    public void checkStreamNotClosed() {
        // none of the tested methods should close the stream
        assertThat(testOutputStream.isClosed(), is(false));
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

        String otherClassifier = "org.eclipse.update.feature";
        Version otherVersion = Version.emptyVersion;
        assertFalse(
                subject.contains(new ArtifactKey(otherClassifier, BUNDLE_A_KEY.getId(), BUNDLE_A_KEY.getVersion())));
        assertFalse(
                subject.contains(new ArtifactKey(BUNDLE_A_KEY.getClassifier(), BUNDLE_A_KEY.getId(), otherVersion)));
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
        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, okStatus());
        assertThat(testSink.getFilesInZip(), is(BUNDLE_A_FILES));
    }

    @Test
    public void testGetArtifactOnlyAvailableInPackedRawFormat() throws Exception {
        Assume.assumeTrue("pack200 not available on current Java version", Runtime.version().feature() < 14);
        testSink = newArtifactSinkFor(BUNDLE_B_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, okStatus());
        assertThat(testSink.getFilesInZip(), is(BUNDLE_B_FILES));
    }

    @Test
    public void testGetNonExistingArtifact() throws Exception {
        testSink = newArtifactSinkFor(NOT_CONTAINED_ARTIFACT_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
        assertThat(status, statusWithMessageWhich(containsString("is not available in")));
    }

    @Test
    public void testGetArtifactSucceedsInSecondAttempt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT, REPO_BUNDLE_A);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(status.getMessage(), containsString("Some attempts to read"));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
        assertThat(asList(status.getChildren()), hasItem(okStatus()));
        assertThat(testSink.getFilesInZip(), is(BUNDLE_A_FILES));
    }

    @Test
    public void testGetArtifactFailsInAllAttempts() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT, REPO_BUNDLE_A_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.writeIsCommitted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getMessage(), containsString("All attempts to read"));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
    }

    @Test
    public void testGetArtifactWithNonRestartableSink() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT, REPO_BUNDLE_A);

        IArtifactSink nonRestartableSink = ArtifactSinkFactory.writeToStream(BUNDLE_A_KEY, testOutputStream);
        status = subject.getArtifact(nonRestartableSink, null);

        // first read attempt fails -> operation fails
        assertThat(status, is(errorStatus()));
        assertThat(status.getMessage(), containsString("An error occurred while transferring artifact")); // original message from p2 as top-level status
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetArtifactToClosedSink() throws Exception {
        subject.getArtifact(new NonStartableArtifactSink(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetArtifactToNonCanonicalSink() throws Exception {
        subject.getArtifact(newRawArtifactSinkFor(packedDescriptorFor(BUNDLE_A_KEY)), null);
    }

    @Test
    public void testGetRawArtifact() throws Exception {
        rawTestSink = newRawArtifactSinkFor(packedDescriptorFor(BUNDLE_B_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, okStatus());
        assertThat(rawTestSink.md5AsHex(), is(BUNDLE_B_PACKED_CONTENT_MD5));
    }

    @Test
    public void testGetNonExistingRawArtifact() throws Exception {
        rawTestSink = new ProbeRawArtifactSink(canonicalDescriptorFor(NOT_CONTAINED_ARTIFACT_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
        assertThat(status, statusWithMessageWhich(containsString("is not available in")));
    }

    @Test
    public void testGetRawArtifactSucceedsInSecondAttempt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);
        rawTestSink = new ProbeRawArtifactSink(packedDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(status.getMessage(), containsString("Some attempts to read"));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
        assertThat(asList(status.getChildren()), hasItem(okStatus()));
        assertThat(rawTestSink.md5AsHex(), is(BUNDLE_A_PACKED_CONTENT_MD5));
    }

    @Test
    public void testGetRawArtifactWithNonRestartableSink() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);

        IRawArtifactSink nonRestartableSink = ArtifactSinkFactory.rawWriteToStream(packedDescriptorFor(BUNDLE_A_KEY),
                testOutputStream);
        status = subject.getRawArtifact(nonRestartableSink, null);

        // first read attempt fails -> operation fails
        assertThat(status, is(errorStatus()));
        assertThat(status.getMessage(), containsString("An error occurred while transferring artifact")); // original message from p2 as top-level status
    }

    @Test
    public void testGetArtifactDescriptors() {
        List<IArtifactDescriptor> descriptors = Arrays.asList(subject.getArtifactDescriptors(BUNDLE_B_KEY));

        assertThat(descriptors, hasItem(inPackedFormat()));
        assertThat(descriptors.size(), is(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRawArtifactToClosedSink() throws Exception {
        subject.getRawArtifact(new NonStartableArtifactSink(), null);
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
        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.md5AsHex(), is(BUNDLE_A_CONTENT_MD5)); // raw artifacts are never processed -> files should be identical
    }

    @Test
    public void testGetPackedRawArtifact() throws Exception {
        IArtifactDescriptor packedBundleB = subject.getArtifactDescriptors(BUNDLE_B_KEY)[0];
        rawTestSink = newRawArtifactSinkFor(packedBundleB);
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.md5AsHex(), is(TestRepositoryContent.BUNDLE_B_PACKED_CONTENT_MD5));
    }

}
