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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LocalArtifactRepositoryP2APITest {

    // bundle already in local repository
    // TODO revise TestRepositoryContent class
    private static final IArtifactKey ARTIFACT_A_KEY = TestRepositoryContent.BUNDLE_A_KEY;
    private static final IArtifactKey ARTIFACT_B_KEY = TestRepositoryContent.BUNDLE_B_KEY;

    private static final IArtifactDescriptor ARTIFACT_A_CANONICAL = localDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_A_PACKED = localPackedDescriptorFor(ARTIFACT_A_KEY);
    private static final IArtifactDescriptor ARTIFACT_B_PACKED = localPackedDescriptorFor(ARTIFACT_B_KEY);
    // not in the repository!
    private static final IArtifactDescriptor ARTIFACT_B_CANONICAL = localDescriptorFor(ARTIFACT_B_KEY);

    private static final IArtifactDescriptor ARTIFACT_A_DESCRIPTOR_1 = ARTIFACT_A_CANONICAL;
    private static final IArtifactDescriptor ARTIFACT_A_DESCRIPTOR_2 = ARTIFACT_A_PACKED;
    private static final IArtifactDescriptor ARTIFACT_B_DESCRIPTOR = ARTIFACT_B_PACKED;

    private static final IArtifactKey OTHER_KEY = TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
    private static final IArtifactDescriptor OTHER_DESCRIPTOR = ARTIFACT_B_CANONICAL;

    private static final Set<IArtifactKey> ORIGINAL_KEYS = new HashSet<IArtifactKey>(Arrays.asList(ARTIFACT_A_KEY,
            ARTIFACT_B_KEY));
    private static final Set<IArtifactDescriptor> ORIGINAL_DESCRIPTORS = new HashSet<IArtifactDescriptor>(
            Arrays.asList(ARTIFACT_A_CANONICAL, ARTIFACT_A_PACKED, ARTIFACT_B_PACKED));

    @Rule
    public TemporaryLocalMavenRepository temporaryLocalMavenRepo = new TemporaryLocalMavenRepository();

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
