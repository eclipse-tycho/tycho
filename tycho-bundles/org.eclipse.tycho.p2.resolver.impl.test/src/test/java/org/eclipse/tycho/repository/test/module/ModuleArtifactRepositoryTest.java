/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test.module;

import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.allKeysIn;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.p2.repository.module.ModuleArtifactRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.repository.test.ResourceUtil;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class ModuleArtifactRepositoryTest {

    private static final IArtifactKey BUNDLE_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "bundle",
            Version.parseVersion("1.2.3.201011101425"));

    private static final int BUNDLE_ARTIFACT_SIZE = 1841;

    private static final IArtifactKey SOURCE_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "bundle.source",
            Version.parseVersion("1.2.3.TAGNAME"));

    private static final int SOURCE_ARTIFACT_SIZE = 418;

    private static final ArtifactKey BINARY_ARTIFACT_KEY = new ArtifactKey("binary", "product.native.launcher",
            Version.parseVersion("0.1.2"));

    private static final int BINARY_ARTIFACT_SIZE = 4;

    private static File existingModuleDir;

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();

    private ModuleArtifactRepository subject;

    @BeforeClass
    public static void initBasicRepository() throws Exception {
        // this folder contains a ModuleArtifactRepository with BUNDLE_ARTIFACT_KEY and SOURCE_ARTIFACT_KEY ...
        existingModuleDir = ResourceUtil.resourceFile("repositories/module/basic/target");

        // ... except for the binary files
        generateDefaultRepositoryArtifacts(existingModuleDir);
    }

    @Test
    public void testLoadRepository() throws Exception {
        subject = ModuleArtifactRepository.restoreInstance(null, existingModuleDir);

        assertEquals(BUNDLE_ARTIFACT_SIZE, artifactSizeOf(BUNDLE_ARTIFACT_KEY, subject));
        assertEquals(SOURCE_ARTIFACT_SIZE, artifactSizeOf(SOURCE_ARTIFACT_KEY, subject));
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        subject = (ModuleArtifactRepository) loadRepositoryViaAgent(existingModuleDir);

        assertEquals(1, subject.getArtifactDescriptors(SOURCE_ARTIFACT_KEY).length);
    }

    @Test(expected = ProvisionException.class)
    public void testLoadRepositoryWithMissingGAVProperties() throws Exception {
        // repository with a missing groupId in one of the descriptors -> loading should fail
        File corruptRepository = ResourceUtil.resourceFile("repositories/module/missingGAV/target");
        generateDefaultRepositoryArtifacts(corruptRepository);

        try {
            subject = (ModuleArtifactRepository) loadRepositoryViaAgent(corruptRepository);
        } catch (ProvisionException e) {
            assertEquals(ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
            assertThat(e.getStatus().getMessage(), containsString("Error while reading repository"));
            assertThat(e.getStatus().getMessage(), containsString("Maven coordinate properties are missing"));
            throw e;
        }
    }

    @Test
    public void testCreateRepository() throws Exception {
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));

        assertTrue(allKeysIn(subject).isEmpty());
    }

    @Test
    public void testWriteToRepository() throws Exception {
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));
        subject.setGAV("", "", ""); // TODO this should not be necessary

        IArtifactSink sink = subject.newAddingArtifactSink(BINARY_ARTIFACT_KEY, new WriteSessionStub());
        writeAndClose(sink.beginWrite(), BINARY_ARTIFACT_SIZE);
        sink.commitWrite();

        assertEquals(BINARY_ARTIFACT_SIZE, artifactSizeOf(BINARY_ARTIFACT_KEY, subject));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteToRepositoryViaStream() throws Exception {
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));

        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        assertEquals(BINARY_ARTIFACT_SIZE, artifactSizeOf(BINARY_ARTIFACT_KEY, subject));
    }

    @Test
    public void testPersistEmptyRepository() throws Exception {
        File repoDir = tempManager.newFolder("targetDir");
        subject = ModuleArtifactRepository.createInstance(null, repoDir);

        IArtifactRepository result = loadRepositoryViaAgent(repoDir);
        assertTrue(allKeysIn(result).isEmpty());
    }

    @Test
    public void testPersistRepository() throws Exception {
        File repoDir = tempManager.newFolder("targetDir");
        subject = ModuleArtifactRepository.createInstance(null, repoDir);

        // TODO write via sink
        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        IArtifactRepository result = loadRepositoryViaAgent(repoDir);
        assertEquals(BINARY_ARTIFACT_SIZE, artifactSizeOf(BINARY_ARTIFACT_KEY, result));
    }

    @Test
    public void testReadingWithOtherDescriptorType() throws Exception {
        subject = ModuleArtifactRepository.restoreInstance(null, existingModuleDir);

        IArtifactDescriptor originalDescriptor = subject.getArtifactDescriptors(BUNDLE_ARTIFACT_KEY)[0];
        IArtifactDescriptor equivalentDescriptor = new ArtifactDescriptor(originalDescriptor);

        assertTrue(subject.contains(BUNDLE_ARTIFACT_KEY));
        assertTrue(subject.contains(originalDescriptor));
        assertTrue(subject.contains(equivalentDescriptor));
    }

    @Test
    public void testRemovingWithOtherDescriptorType() throws Exception {
        // existingModuleDir points to the original source files -> use temporary repository instead so that we don't edit source files
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));
        // TODO write via sink
        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        IArtifactDescriptor originalDescriptor = subject.getArtifactDescriptors(BINARY_ARTIFACT_KEY)[0];
        IArtifactDescriptor equivalentDescriptor = new ArtifactDescriptor(originalDescriptor);

        // self-test: now the key/descriptor should be contained
        assertTrue(subject.contains(BINARY_ARTIFACT_KEY));
        assertTrue(subject.contains(originalDescriptor));
        assertTrue(subject.contains(equivalentDescriptor));

        subject.removeDescriptor(equivalentDescriptor);

        assertFalse(subject.contains(equivalentDescriptor));
        assertFalse(subject.contains(originalDescriptor));
        assertFalse(subject.contains(BUNDLE_ARTIFACT_KEY));
    }

    private IArtifactDescriptor newDescriptor(ArtifactKey artifactKey) {
        // TODO this is wrong
        subject.setGAV("", "", "");

        return subject.createArtifactDescriptor(artifactKey, new WriteSessionStub());
    }

    private static int artifactSizeOf(IArtifactKey artifactKey, IArtifactRepository subject) {
        IArtifactDescriptor[] artifactDescriptors = subject.getArtifactDescriptors(artifactKey);
        assertEquals(1, artifactDescriptors.length);

        ByteArrayOutputStream artifactContent = new ByteArrayOutputStream();
        subject.getArtifact(artifactDescriptors[0], artifactContent, null);
        return artifactContent.size();
    }

    private IArtifactRepository loadRepositoryViaAgent(File location) throws ProvisionException {
        IArtifactRepositoryManager repoManager = p2Context.getService(IArtifactRepositoryManager.class);
        return repoManager.loadRepository(location.toURI(), null);
    }

    static void writeAndClose(OutputStream out, int size) throws IOException {
        byte[] content = new byte[size];
        Arrays.fill(content, (byte) 'b');
        out.write(content);
        out.flush();
        out.close();
    }

    private static void generateDefaultRepositoryArtifacts(File location) throws IOException {
        generateBinaryTestFile(new File(location, "the-bundle.jar"), BUNDLE_ARTIFACT_SIZE);
        generateBinaryTestFile(new File(location, "the-sources.jar"), SOURCE_ARTIFACT_SIZE);
    }

    private static void generateBinaryTestFile(File file, int size) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            OutputStream os = new BufferedOutputStream(fos);
            for (int i = 0; i < size; ++i) {
                os.write(0);
            }
            os.flush();
        }
        file.deleteOnExit();
    }

    static class WriteSessionStub implements WriteSessionContext {

        @Override
        public ClassifierAndExtension getClassifierAndExtensionForNewKey(IArtifactKey key) {
            return new ClassifierAndExtension(key.getId(), "jar");
        }
    }
}
