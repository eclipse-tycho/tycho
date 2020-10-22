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
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;
import org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink;
import org.eclipse.tycho.repository.streaming.testutil.ProbeOutputStream;
import org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests of the general requirements from the {@link IRawArtifactFileProvider} and p2
 * {@link IArtifactRepository} interfaces. (So most of the code under test is located in
 * {@link LocalArtifactRepository}'s base class {@link ArtifactRepositoryBaseImpl}.)
 * 
 * The characteristics specific to the {@link LocalArtifactRepository} implementation are tested in
 * {@link LocalArtifactRepositoryTest}.
 */
public class LocalArtifactRepositoryP2APITest {

    // bundle already in local repository
    private static final IArtifactKey ARTIFACT_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey ARTIFACT_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;

    private static final Set<String> ARTIFACT_A_CONTENT = TestRepositoryContent.BUNDLE_A_FILES;
    private static final Set<String> ARTIFACT_B_CONTENT = TestRepositoryContent.BUNDLE_B_FILES;

    private static final IArtifactDescriptor ARTIFACT_A_CANONICAL = localCanonicalDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_A_PACKED = localPackedDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_B_PACKED = localPackedDescriptorFor(ARTIFACT_B_KEY);
    // not in the repository!
    private static final IArtifactDescriptor ARTIFACT_B_CANONICAL = localCanonicalDescriptorFor(ARTIFACT_B_KEY);

    private static final String ARTIFACT_A_CANONICAL_MD5 = TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
    private static final String ARTIFACT_A_PACKED_MD5 = TestRepositoryContent.BUNDLE_A_PACKED_CONTENT_MD5;

    private static final IArtifactDescriptor ARTIFACT_A_DESCRIPTOR_1 = ARTIFACT_A_CANONICAL;
    private static final IArtifactDescriptor ARTIFACT_A_DESCRIPTOR_2 = ARTIFACT_A_PACKED;
    private static final IArtifactDescriptor ARTIFACT_B_DESCRIPTOR = ARTIFACT_B_PACKED;

    // not in the repository
    private static final IArtifactKey OTHER_KEY = TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
    private static final IArtifactDescriptor OTHER_DESCRIPTOR = ARTIFACT_B_CANONICAL;

    // not (yet) in the repository
    private static final IArtifactKey NEW_KEY = TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
    private static final IArtifactDescriptor NEW_DESCRIPTOR = localPackedDescriptorFor(NEW_KEY);

    private static final Set<IArtifactKey> ORIGINAL_KEYS = new HashSet<>(Arrays.asList(ARTIFACT_A_KEY, ARTIFACT_B_KEY));
    private static final Set<IArtifactDescriptor> ORIGINAL_DESCRIPTORS = new HashSet<>(
            Arrays.asList(ARTIFACT_A_CANONICAL, ARTIFACT_A_PACKED, ARTIFACT_B_PACKED));

    @Rule
    public TemporaryLocalMavenRepository temporaryLocalMavenRepo = new TemporaryLocalMavenRepository();

    private ProbeArtifactSink testSink;
    private ProbeRawArtifactSink rawTestSink;
    private ProbeOutputStream testOutputStream;

    private LocalArtifactRepository subject;
    private IStatus status;

    @Before
    public void initSubject() throws Exception {
        temporaryLocalMavenRepo.initContentFromResourceFolder(ResourceUtil.resourceFile("repositories/local"));
        subject = new LocalArtifactRepository(null, temporaryLocalMavenRepo.getLocalRepositoryIndex());

        testOutputStream = new ProbeOutputStream();
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
        assertTrue(subject.contains(ARTIFACT_A_KEY));
        assertTrue(subject.contains(ARTIFACT_B_KEY));

        assertFalse(subject.contains(OTHER_KEY));
    }

    @Test
    public void testContainsDescriptor() {
        assertTrue(subject.contains(ARTIFACT_A_DESCRIPTOR_1));
        assertTrue(subject.contains(foreignEquivalentOf(ARTIFACT_A_DESCRIPTOR_1)));

        assertFalse(subject.contains(OTHER_DESCRIPTOR));
    }

    @Test
    public void testGetDescriptors() {
        List<IArtifactDescriptor> result = Arrays.asList(subject.getArtifactDescriptors(ARTIFACT_A_KEY));

        assertThat(result, hasItem(ARTIFACT_A_DESCRIPTOR_1));
        assertThat(result, hasItem(ARTIFACT_A_DESCRIPTOR_2));
        assertThat(result.size(), is(2));
    }

    @Test
    public void testGetDescriptorsOfNonContainedKey() {
        List<IArtifactDescriptor> result = Arrays.asList(subject.getArtifactDescriptors(OTHER_KEY));

        assertThat(result, notNullValue());
        assertThat(result.size(), is(0));
    }

    @Test
    public void testQueryKeys() {
        Set<IArtifactKey> result = allKeysIn(subject);

        assertThat(result, hasItem(ARTIFACT_A_KEY));
        assertThat(result, hasItem(ARTIFACT_B_KEY));
        assertThat(result, is(ORIGINAL_KEYS));
    }

    @Test
    public void testQueryDescriptors() {
        Set<IArtifactDescriptor> result = allDescriptorsIn(subject);

        assertThat(result, hasItem(ARTIFACT_A_DESCRIPTOR_1));
        assertThat(result, hasItem(ARTIFACT_A_DESCRIPTOR_2));
        assertThat(result, hasItem(ARTIFACT_B_DESCRIPTOR));
        assertThat(result, is(ORIGINAL_DESCRIPTORS));
    }

    @Test
    public void testRemoveLastDescriptorOfKey() {
        subject.removeDescriptor(ARTIFACT_B_DESCRIPTOR);

        assertFalse(subject.contains(ARTIFACT_B_DESCRIPTOR));
        assertFalse(subject.contains(ARTIFACT_B_KEY));
        assertTotal(-1, -1);
    }

    @Test
    public void testRemoveOneOfDescriptorsOfKey() {
        subject.removeDescriptor(ARTIFACT_A_DESCRIPTOR_1);

        assertFalse(subject.contains(ARTIFACT_A_DESCRIPTOR_1));
        assertTrue(subject.contains(ARTIFACT_A_DESCRIPTOR_2));
        assertTrue(subject.contains(ARTIFACT_A_KEY));
        assertTotal(0, -1);
    }

    @Test
    public void testRemoveAllDescriptorsOfKey() {
        subject.removeDescriptors(new IArtifactDescriptor[] { ARTIFACT_A_DESCRIPTOR_1, ARTIFACT_A_DESCRIPTOR_2 });

        assertFalse(subject.contains(ARTIFACT_A_DESCRIPTOR_1));
        assertFalse(subject.contains(ARTIFACT_A_DESCRIPTOR_2));
        assertFalse(subject.contains(ARTIFACT_A_KEY));
        assertTotal(-1, -2);
    }

    @Test
    public void testRemoveForeignEquivalentDescriptor() {
        IArtifactDescriptor foreignDescriptor = foreignEquivalentOf(ARTIFACT_B_DESCRIPTOR);
        assertTrue(subject.contains(foreignDescriptor)); // self-test

        subject.removeDescriptor(foreignDescriptor);

        // foreign descriptor must have been replaced by internal descriptor for the calls to the internal HashMap
        assertFalse(subject.contains(foreignDescriptor));
    }

    @Test
    public void testRemoveNonContainedDescriptor() {
        subject.removeDescriptor(OTHER_DESCRIPTOR);

        assertNoChanges();
    }

    @Test
    public void testRemoveKey() {
        subject.removeDescriptor(ARTIFACT_A_KEY);

        assertFalse(subject.contains(ARTIFACT_A_KEY));
        assertFalse(subject.contains(ARTIFACT_A_DESCRIPTOR_1));
        assertFalse(subject.contains(ARTIFACT_A_DESCRIPTOR_2));
        assertTotal(-1, -2);
    }

    @Test
    public void testRemoveKeys() {
        subject.removeDescriptors(new IArtifactKey[] { ARTIFACT_A_KEY, ARTIFACT_B_KEY });

        assertFalse(subject.contains(ARTIFACT_A_KEY));
        assertFalse(subject.contains(ARTIFACT_B_KEY));
        assertTotal(-2, -3);
    }

    @Test
    public void testRemoveNonContainedKey() {
        IArtifactKey keyToRemove = OTHER_KEY;

        subject.removeDescriptor(keyToRemove);

        assertNoChanges();
    }

    @Test
    public void testRemoveAll() {
        subject.removeAll();

        assertThat(allKeysIn(subject).size(), is(0));
        assertThat(allDescriptorsIn(subject).size(), is(0));
        assertTotal(-2, -3);
    }

    @Test
    public void testGetArtifactFile() {
        File result = subject.getArtifactFile(ARTIFACT_A_KEY);

        assertThat(result, is(artifactLocationOf(ARTIFACT_A_KEY, ".jar")));
    }

    @Test
    public void testGetArtifactFileOfNonContainedKey() {
        File result = subject.getArtifactFile(OTHER_KEY);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void testGetArtifactFileOfKeyWithoutCanonicalFormat() {
        assertFalse(subject.contains(ARTIFACT_B_CANONICAL)); // self-test

        File result = subject.getArtifactFile(ARTIFACT_B_KEY);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void testGetRawArtifactFile() {
        File result = subject.getArtifactFile(ARTIFACT_B_PACKED);

        assertThat(result, is(artifactLocationOf(ARTIFACT_B_KEY, "-pack200.jar.pack.gz")));
    }

    @Test
    public void testGetRawArtifactFileOfNonContainedFormat() {
        File result = subject.getArtifactFile(ARTIFACT_B_CANONICAL);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void testGetArtifact() throws Exception {
        testSink = newArtifactSinkFor(ARTIFACT_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.getFilesInZip(), is(ARTIFACT_A_CONTENT));
    }

    @Test
    public void testGetNonContainedArtifact() throws Exception {
        testSink = newArtifactSinkFor(OTHER_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testGetArtifactOnlyAvailableInPackedFormat() throws Exception {
        Assume.assumeTrue("pack200 not available on current Java version", Runtime.version().feature() < 14);
        testSink = newArtifactSinkFor(ARTIFACT_B_KEY);
        // this method must return the original artifact, regardless of how the artifact is stored internally
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.getFilesInZip(), is(ARTIFACT_B_CONTENT));
    }

    @Test
    public void testGetCorruptedArtifact() throws Exception {
        // simulate corruption of the artifact in the file system
        assertThat(artifactLocationOf(ARTIFACT_B_KEY, "-pack200.jar.pack.gz").delete(), is(true)); // this is the only format

        testSink = newArtifactSinkFor(ARTIFACT_B_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(testSink.writeIsCommitted(), is(false));
        assertThat(status, is(errorStatus()));
    }

    @Test(expected = ArtifactSinkException.class)
    public void testGetArtifactToBrokenSink() throws Exception {
        IArtifactSink brokenSink = new IArtifactSink() {
            @Override
            public IArtifactKey getArtifactToBeWritten() {
                return ARTIFACT_A_KEY;
            }

            @Override
            public boolean canBeginWrite() {
                return true;
            }

            @Override
            public OutputStream beginWrite() {
                return testOutputStream;
            }

            @Override
            public void commitWrite() throws ArtifactSinkException {
                throw new ArtifactSinkException("simulated error on commit");
            }

            @Override
            public void abortWrite() {
            }

        };
        subject.getArtifact(brokenSink, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetArtifactToClosedSink() throws Exception {
        subject.getArtifact(new NonStartableArtifactSink(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetArtifactToNonCanonicalSink() throws Exception {
        subject.getArtifact(newRawArtifactSinkFor(ARTIFACT_A_PACKED), null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetArtifactToStream() throws Exception {
        status = subject.getArtifact(ARTIFACT_A_CANONICAL, testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.getFilesInZip(), is(ARTIFACT_A_CONTENT));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetNonContainedArtifactToStream() {
        status = subject.getArtifact(OTHER_DESCRIPTOR, testOutputStream, null);

        assertThat(testOutputStream.writtenBytes(), is(0));
        assertThat(testOutputStream.getStatus(), is(errorStatus())); // from IStateful
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetArtifactToStreamOnlyAvailableInPackedFormat() throws Exception {
        Assume.assumeTrue("pack200 not available on current Java version", Runtime.version().feature() < 14);
        // this method must always return the original artifact, even if called with a pack200 descriptor  
        status = subject.getArtifact(ARTIFACT_B_PACKED, testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.writtenBytes(), not(is(0)));
        assertThat(testOutputStream.getFilesInZip(), is(ARTIFACT_B_CONTENT));
    }

    @Test
    public void testGetRawArtifact() throws Exception {
        rawTestSink = newRawArtifactSinkFor(ARTIFACT_A_PACKED);
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, is(okStatus()));
        assertThat(rawTestSink.md5AsHex(), is(ARTIFACT_A_PACKED_MD5));
    }

    @Test
    public void testGetRawArtifactForCanonicalFormat() throws Exception {
        rawTestSink = newRawArtifactSinkFor(ARTIFACT_A_CANONICAL);
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, is(okStatus()));
        assertThat(rawTestSink.md5AsHex(), is(ARTIFACT_A_CANONICAL_MD5));
    }

    @Test
    public void testGetRawArtifactOfNonContainedFormat() throws Exception {
        assertTrue(subject.contains(ARTIFACT_B_PACKED));
        assertFalse(subject.contains(ARTIFACT_B_CANONICAL));

        // getRawArtifact does not convert from packed to canonical format
        rawTestSink = newRawArtifactSinkFor(ARTIFACT_B_CANONICAL);
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.writeIsStarted(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testGetCorruptedRawArtifact() throws Exception {
        // simulate corruption of the artifact in the file system
        assertThat(artifactLocationOf(ARTIFACT_B_KEY, "-pack200.jar.pack.gz").delete(), is(true));

        rawTestSink = newRawArtifactSinkFor(ARTIFACT_B_PACKED);
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(rawTestSink.writeIsCommitted(), is(false));
        assertThat(status, is(errorStatus()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRawArtifactToClosedSink() throws Exception {
        subject.getRawArtifact(new NonStartableArtifactSink(), null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactToStream() throws Exception {
        status = subject.getRawArtifact(ARTIFACT_A_PACKED, testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.md5AsHex(), is(ARTIFACT_A_PACKED_MD5));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactForCanonicalFormatToStream() throws Exception {
        status = subject.getRawArtifact(ARTIFACT_A_CANONICAL, testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.md5AsHex(), is(ARTIFACT_A_CANONICAL_MD5));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactOfNonContainedFormatToStream() {
        assertFalse(subject.contains(ARTIFACT_B_CANONICAL));

        // getRawArtifact does not convert from packed to canonical format
        status = subject.getRawArtifact(ARTIFACT_B_CANONICAL, testOutputStream, null);

        assertThat(testOutputStream.writtenBytes(), is(0));
        assertThat(testOutputStream.getStatus(), is(errorStatus())); // from IStateful
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testWriteArtifact() throws Exception {
        IArtifactSink addSink = subject.newAddingArtifactSink(NEW_KEY);
        addSink.beginWrite().write(new byte[33]);
        addSink.commitWrite();

        assertTrue(subject.contains(NEW_KEY));
        assertTrue(subject.contains(localCanonicalDescriptorFor(NEW_KEY)));
        assertThat(readSizeOfArtifact(NEW_KEY), is(33));
    }

    @Test
    public void testReWriteArtifactFails() throws Exception {
        // LocalArtifactRepository doesn't allow overwrites -> this may be different in other IArtifactRepository implementations
        ProvisionException expectedException = null;
        try {
            IArtifactSink addSink = subject.newAddingArtifactSink(ARTIFACT_A_KEY);
            addSink.beginWrite();
            addSink.commitWrite();
        } catch (ProvisionException e) {
            expectedException = e;
        }

        assertThat(expectedException, is(instanceOf(ProvisionException.class)));
        assertThat(expectedException.getStatus().getCode(), is(ProvisionException.ARTIFACT_EXISTS));
    }

    @Test
    public void testWriteArtifactAndCancel() throws Exception {
        IArtifactSink addSink = subject.newAddingArtifactSink(NEW_KEY);
        addSink.beginWrite().write(new byte[33]);
        addSink.abortWrite();

        assertFalse(subject.contains(NEW_KEY));
        assertFalse(subject.contains(localCanonicalDescriptorFor(NEW_KEY)));
    }

    @Test
    public void testWriteArtifactOnSecondAttempt() throws Exception {
        IArtifactSink addSink = subject.newAddingArtifactSink(NEW_KEY);
        addSink.beginWrite().write(new byte[11]);
        addSink.beginWrite().write(new byte[22]);
        addSink.commitWrite();

        assertThat(subject.contains(NEW_KEY), is(true));
        assertThat(subject.contains(localCanonicalDescriptorFor(NEW_KEY)), is(true));
        assertThat(readSizeOfArtifact(NEW_KEY), is(22));
    }

    @Test
    public void testWriteRawArtifact() throws Exception {
        IRawArtifactSink addSink = subject.newAddingRawArtifactSink(NEW_DESCRIPTOR);
        addSink.beginWrite().write(new byte[33]);
        addSink.commitWrite();

        assertThat(subject.contains(NEW_DESCRIPTOR), is(true));
        assertThat(subject.contains(NEW_DESCRIPTOR.getArtifactKey()), is(true));
        assertThat(readSizeOfRawArtifact(NEW_DESCRIPTOR), is(33));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteArtifactViaStream() throws Exception {
        try (OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR))) {
            addSink.write(new byte[33]);
        }

        assertThat(subject.contains(NEW_KEY), is(true));
        assertThat(subject.contains(NEW_DESCRIPTOR), is(true));
        subject.getRawArtifact(NEW_DESCRIPTOR, testOutputStream, null);
        assertThat(testOutputStream.writtenBytes(), is(33));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testReWriteArtifactViaStreamFails() throws Exception {
        ProvisionException expectedException = null;

        try (OutputStream addSink = subject.getOutputStream(ARTIFACT_A_CANONICAL)) {
            addSink.write(new byte[1]);
        } catch (ProvisionException e) {
            expectedException = e;
        }

        assertThat(expectedException, is(instanceOf(ProvisionException.class)));
        assertThat(expectedException.getStatus().getCode(), is(ProvisionException.ARTIFACT_EXISTS));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteArtifactViaStreamAndCancel() throws Exception {
        try (OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR))) {
            addSink.write(new byte[33]);
            // setStatus needs to be called when copying from a repository using getArtifact, and that method returns an error (e.g. due to artifact corruption)
            ((IStateful) addSink).setStatus(new Status(IStatus.ERROR, "test", "written data is bad"));
        }

        assertThat(subject.contains(NEW_DESCRIPTOR), is(false));
        assertThat(subject.contains(NEW_KEY), is(false));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteArtifactViaStreamWithNonFatalStatus() throws Exception {
        try (OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR))) {
            addSink.write(new byte[33]);
            ((IStateful) addSink).setStatus(new Status(IStatus.WARNING, "test", "irrelevant warning"));
        }

        assertThat(subject.contains(NEW_DESCRIPTOR), is(true));
    }

    /**
     * Returns a descriptor of the internally used {@link IArtifactDescriptor} type for the
     * canonical format of the given key.
     */
    private static IArtifactDescriptor localCanonicalDescriptorFor(IArtifactKey key) {
        return new GAVArtifactDescriptor(key);
    }

    /**
     * Returns a descriptor of the internally used {@link IArtifactDescriptor} type for the pack200
     * format of the given key.
     */
    private static IArtifactDescriptor localPackedDescriptorFor(IArtifactKey key) {
        GAVArtifactDescriptor result = new GAVArtifactDescriptor(key);
        result.setProcessingSteps(new IProcessingStepDescriptor[] {
                new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) });
        result.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
        return result;
    }

    /**
     * Returns a descriptor of the a different {@link IArtifactDescriptor} type descriptor.
     * Instances of the returned type never equal the internally used type.
     * 
     * @see ArtifactDescriptor#equals(Object)
     */
    private static IArtifactDescriptor foreignEquivalentOf(IArtifactDescriptor descriptor) {
        return new ArtifactDescriptor(descriptor);
    }

    private void assertNoChanges() {
        assertThat(allKeysIn(subject), is(ORIGINAL_KEYS));
        assertThat(allDescriptorsIn(subject), is(ORIGINAL_DESCRIPTORS));
    }

    private void assertTotal(int keyDiff, int descriptorDiff) {
        assertThat(allKeysIn(subject).size(), is(ORIGINAL_KEYS.size() + keyDiff));
        assertThat(allDescriptorsIn(subject).size(), is(ORIGINAL_DESCRIPTORS.size() + descriptorDiff));
    }

    private static Set<IArtifactKey> allKeysIn(LocalArtifactRepository repository) {
        return repository.query(QueryUtil.createMatchQuery(IArtifactKey.class, "true"), null).toUnmodifiableSet();
    }

    private static Set<IArtifactDescriptor> allDescriptorsIn(LocalArtifactRepository repository) {
        return repository.descriptorQueryable()
                .query(QueryUtil.createMatchQuery(IArtifactDescriptor.class, "true"), null).toUnmodifiableSet();
    }

    private File artifactLocationOf(IArtifactKey key, String classifierAndExtension) {
        return new File(temporaryLocalMavenRepo.getLocalRepositoryRoot(),
                "p2/" + key.getClassifier().replace('.', '/') + "/" + key.getId() + "/" + key.getVersion() + "/"
                        + key.getId() + "-" + key.getVersion() + classifierAndExtension);
    }

    private int readSizeOfArtifact(IArtifactKey key) throws ArtifactSinkException {
        // don't use the member here because getArtifact is not the method under test here
        ProbeArtifactSink temporarySink = newArtifactSinkFor(key);

        subject.getArtifact(temporarySink, null);
        return temporarySink.committedBytes();
    }

    private int readSizeOfRawArtifact(IArtifactDescriptor descriptor) throws ArtifactSinkException {
        // don't use the member here because getArtifact is not the method under test here
        ProbeRawArtifactSink temporarySink = newRawArtifactSinkFor(descriptor);

        subject.getRawArtifact(temporarySink, null);
        return temporarySink.committedBytes();
    }
}
