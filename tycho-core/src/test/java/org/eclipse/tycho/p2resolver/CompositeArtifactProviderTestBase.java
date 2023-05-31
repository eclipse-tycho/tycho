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

import static java.util.Arrays.asList;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.inCanonicalFormat;
import static org.eclipse.tycho.test.util.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.test.util.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.statusWithMessageWhich;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_FILES;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_A;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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
import org.eclipse.tycho.IArtifactSink;
import org.eclipse.tycho.IRawArtifactProvider;
import org.eclipse.tycho.IRawArtifactSink;
import org.eclipse.tycho.p2.repository.streaming.ArtifactSinkFactory;
import org.eclipse.tycho.test.util.NonStartableArtifactSink;
import org.eclipse.tycho.test.util.ProbeArtifactSink;
import org.eclipse.tycho.test.util.ProbeOutputStream;
import org.eclipse.tycho.test.util.ProbeRawArtifactSink;
import org.eclipse.tycho.test.util.TestRepositoryContent;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class CompositeArtifactProviderTestBase<T extends IRawArtifactProvider> extends TychoPlexusTestCase {

    protected ProbeArtifactSink testSink;
    protected ProbeRawArtifactSink rawTestSink;
    protected ProbeOutputStream testOutputStream = new ProbeOutputStream();

    protected T subject;
    protected IStatus status;

    protected abstract T createCompositeArtifactProvider(URI... repositoryURLs) throws Exception;

    @Before
    public void initContextAndSubject() throws Exception {
        subject = createCompositeArtifactProvider(TestRepositoryContent.REPO2_BUNDLE_A,
                TestRepositoryContent.REPO_BUNDLE_AB);
    }

    @After
    public void checkStreamNotClosed() {
        // none of the tested methods should close the stream
        assertFalse(testOutputStream.isClosed());
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
        assertTrue(subject.contains(TestRepositoryContent.BUNDLE_A_KEY));
        assertTrue(subject.contains(TestRepositoryContent.BUNDLE_B_KEY));

        String otherClassifier = "org.eclipse.update.feature";
        Version otherVersion = Version.emptyVersion;
        assertFalse(subject.contains(new ArtifactKey(otherClassifier, TestRepositoryContent.BUNDLE_A_KEY.getId(),
                TestRepositoryContent.BUNDLE_A_KEY.getVersion())));
        assertFalse(
                subject.contains(new ArtifactKey(BUNDLE_A_KEY.getClassifier(), BUNDLE_A_KEY.getId(), otherVersion)));
    }

    @Test
    public void testQuery() {
        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertEquals(2, result.toSet().size());
    }

    @Test
    public void testQueryWithSingleRepository() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A);

        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertEquals(1, result.toSet().size());
    }

    @Test
    public void testQueryWithoutRepositories() throws Exception {
        subject = createCompositeArtifactProvider(new URI[0]);

        IQueryResult<IArtifactKey> result = subject.query(ANY_ARTIFACT_KEY_QUERY, null);
        assertTrue(result.toSet().isEmpty());
    }

    @Test
    public void testGetArtifact() throws Exception {
        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertTrue(status.isOK());
        assertEquals(BUNDLE_A_FILES, testSink.getFilesInZip());
    }

    @Test
    public void testGetNonExistingArtifact() throws Exception {
        testSink = newArtifactSinkFor(NOT_CONTAINED_ARTIFACT_KEY);
        status = subject.getArtifact(testSink, null);

        assertFalse(testSink.writeIsStarted());
        assertThat(status, is(errorStatus()));
        assertEquals(ProvisionException.ARTIFACT_NOT_FOUND, status.getCode());
        assertThat(status, statusWithMessageWhich(containsString("is not available in")));
    }

    @Test
    public void testGetArtifactSucceedsInSecondAttempt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT, TestRepositoryContent.REPO2_BUNDLE_A);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(status.getMessage(), containsString("Some attempts to read"));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
        assertThat(asList(status.getChildren()), hasItem(okStatus()));
        assertEquals(BUNDLE_A_FILES, testSink.getFilesInZip());
    }

    @Test
    public void testGetArtifactFailsInAllAttempts() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT, REPO_BUNDLE_A_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertFalse(testSink.writeIsCommitted());
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

    @Test
    public void testGetArtifactToNonCanonicalSink() throws Exception {
        subject.getArtifact(newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY)), null);
    }

    @Test
    public void testGetNonExistingRawArtifact() throws Exception {
        rawTestSink = new ProbeRawArtifactSink(canonicalDescriptorFor(NOT_CONTAINED_ARTIFACT_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertFalse(rawTestSink.writeIsStarted());
        assertThat(status, is(errorStatus()));
        assertEquals(ProvisionException.ARTIFACT_NOT_FOUND, status.getCode());
        assertThat(status, statusWithMessageWhich(containsString("is not available in")));
    }

    @Test
    public void testGetRawArtifactSucceedsInSecondAttempt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);
        rawTestSink = new ProbeRawArtifactSink(canonicalDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertTrue(status.isOK());
        assertEquals(BUNDLE_A_CONTENT_MD5, rawTestSink.md5AsHex());
    }

    @Test
    public void testGetRawArtifactWithNonRestartableSink() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);

        IRawArtifactSink nonRestartableSink = ArtifactSinkFactory.rawWriteToStream(canonicalDescriptorFor(BUNDLE_A_KEY),
                testOutputStream);
        status = subject.getRawArtifact(nonRestartableSink, null);

        assertTrue(status.isOK());
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
        assertEquals(2, result.size()); // no duplicates
    }

    @Test
    public void testContainsArtifactDescriptor() {
        assertTrue(subject.contains(canonicalDescriptorFor(BUNDLE_A_KEY)));
        assertFalse(subject.contains(canonicalDescriptorFor(BUNDLE_B_KEY)));
    }

    @Test
    public void testGetCanonicalRawArtifact() throws Exception {
        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertEquals(BUNDLE_A_CONTENT_MD5, rawTestSink.md5AsHex()); // raw artifacts are never processed -> files should be identical
    }

}
