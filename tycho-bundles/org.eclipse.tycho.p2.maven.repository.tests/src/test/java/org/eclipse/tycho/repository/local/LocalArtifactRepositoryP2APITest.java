/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;

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
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.test.util.ProbeOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LocalArtifactRepositoryP2APITest {

    // bundle already in local repository
    // TODO revise TestRepositoryContent class
    private static final IArtifactKey ARTIFACT_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey ARTIFACT_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;

    private static final Set<String> ARTIFACT_A_CONTENT = TestRepositoryContent.BUNDLE_A_FILES;
    private static final Set<String> ARTIFACT_B_CONTENT = TestRepositoryContent.BUNDLE_B_FILES;

    private static final IArtifactDescriptor ARTIFACT_A_CANONICAL = localDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_A_PACKED = localPackedDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_B_PACKED = localPackedDescriptorFor(ARTIFACT_B_KEY);
    // not in the repository!
    private static final IArtifactDescriptor ARTIFACT_B_CANONICAL = localDescriptorFor(ARTIFACT_B_KEY);

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

    private static final Set<IArtifactKey> ORIGINAL_KEYS = new HashSet<IArtifactKey>(Arrays.asList(ARTIFACT_A_KEY,
            ARTIFACT_B_KEY));
    private static final Set<IArtifactDescriptor> ORIGINAL_DESCRIPTORS = new HashSet<IArtifactDescriptor>(
            Arrays.asList(ARTIFACT_A_CANONICAL, ARTIFACT_A_PACKED, ARTIFACT_B_PACKED));

    @Rule
    public TemporaryLocalMavenRepository temporaryLocalMavenRepo = new TemporaryLocalMavenRepository();
    private ProbeOutputStream testSink = new ProbeOutputStream();

    private LocalArtifactRepository subject;

    @Before
    public void initSubject() throws Exception {
        temporaryLocalMavenRepo.initContentFromTestResource("repositories/local");
        subject = new LocalArtifactRepository(null, temporaryLocalMavenRepo.getLocalRepositoryIndex());
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
        IStatus status = subject.getArtifact(ARTIFACT_A_KEY, testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.isClosed(), is(false));
        assertThat(testSink.getFilesInZip(), is(ARTIFACT_A_CONTENT));
    }

    @Test
    public void testGetNonContainedArtifact() {
        IStatus status = subject.getArtifact(OTHER_KEY, testSink, null);

        assertThat(testSink.isClosed(), is(false));
        assertThat(testSink.writtenBytes(), is(0));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testGetArtifactOnlyAvailableInPackedFormat() throws Exception {
        // this method must return the original artifact, regardless of how the artifact is stored internally
        IStatus status = subject.getArtifact(ARTIFACT_B_KEY, testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.isClosed(), is(false));
        assertThat(testSink.writtenBytes(), not(is(0)));
        assertThat(testSink.getFilesInZip(), is(ARTIFACT_B_CONTENT));
    }

    @Test
    public void testGetRawArtifact() throws Exception {
        IStatus status = subject.getRawArtifact(ARTIFACT_A_PACKED, testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.isClosed(), is(false));
        assertThat(testSink.md5AsHex(), is(ARTIFACT_A_PACKED_MD5));
    }

    @Test
    public void testGetRawArtifactForCanonicalFormat() throws Exception {
        IStatus status = subject.getRawArtifact(ARTIFACT_A_CANONICAL, testSink, null);

        assertThat(status, is(okStatus()));
        assertThat(testSink.isClosed(), is(false));
        assertThat(testSink.md5AsHex(), is(ARTIFACT_A_CANONICAL_MD5));
    }

    @Test
    public void testGetRawArtifactOfNonContainedFormat() {
        assertFalse(subject.contains(ARTIFACT_B_CANONICAL)); // self-test

        IStatus status = subject.getRawArtifact(ARTIFACT_B_CANONICAL, testSink, null);

        assertThat(testSink.writtenBytes(), is(0));
        assertThat(testSink.isClosed(), is(false));
        assertThat(status, is(errorStatus()));
        assertThat(status.getCode(), is(ProvisionException.ARTIFACT_NOT_FOUND));
    }

    @Test
    public void testWriteArtifact() throws Exception {
        OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR));
        addSink.write(new byte[33]);
        addSink.close();

        assertTrue(subject.contains(NEW_KEY));
        assertTrue(subject.contains(NEW_DESCRIPTOR));
        subject.getRawArtifact(NEW_DESCRIPTOR, testSink, null);
        assertThat(testSink.writtenBytes(), is(33));
    }

    @Test
    public void testReWriteArtifactFails() throws Exception {
        ProvisionException expectedException = null;
        try {
            OutputStream addSink = subject.getOutputStream(ARTIFACT_A_CANONICAL);
            addSink.write(new byte[1]);
            addSink.close();
        } catch (ProvisionException e) {
            expectedException = e;
        }

        assertThat(expectedException, is(ProvisionException.class));
        assertThat(expectedException.getStatus().getCode(), is(ProvisionException.ARTIFACT_EXISTS));
    }

    @Test
    public void testWriteArtifactAndCancel() throws Exception {
        OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR));
        addSink.write(new byte[33]);
        // setStatus needs to be called when copying from a repository using getArtifact, and that method returns an error (e.g. due to artifact corruption)
        ((IStateful) addSink).setStatus(new Status(IStatus.ERROR, "test", "written data is bad"));
        addSink.close();

        assertFalse(subject.contains(NEW_DESCRIPTOR));
        assertFalse(subject.contains(NEW_KEY));
    }

    @Test
    public void testWriteArtifactWithNonFatalStatus() throws Exception {
        OutputStream addSink = subject.getOutputStream(foreignEquivalentOf(NEW_DESCRIPTOR));
        addSink.write(new byte[33]);
        ((IStateful) addSink).setStatus(new Status(IStatus.WARNING, "test", "irrelevant warning"));
        addSink.close();

        assertTrue(subject.contains(NEW_DESCRIPTOR));
    }

    /**
     * Returns a descriptor of the internally used {@link IArtifactDescriptor} type for the
     * canonical format of the given key.
     */
    private static IArtifactDescriptor localDescriptorFor(IArtifactKey key) {
        return new GAVArtifactDescriptor(key);
    }

    /**
     * Returns a descriptor of the internally used {@link IArtifactDescriptor} type for the pack200
     * format of the given key.
     */
    private static IArtifactDescriptor localPackedDescriptorFor(IArtifactKey key) {
        GAVArtifactDescriptor result = new GAVArtifactDescriptor(key);
        result.setProcessingSteps(new IProcessingStepDescriptor[] { new ProcessingStepDescriptor(
                "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) });
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
        return new File(temporaryLocalMavenRepo.getLocalRepositoryRoot(), "p2/" + key.getClassifier().replace('.', '/')
                + "/" + key.getId() + "/" + key.getVersion() + "/" + key.getId() + "-" + key.getVersion()
                + classifierAndExtension);
    }
}
