/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module;

import static org.eclipse.tycho.repository.test.util.ArtifactRepositoryUtils.allKeysIn;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
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
    public static void initExistingRepository() throws Exception {
        // this folder contains a ModuleArtifactRepository with BUNDLE_ARTIFACT_KEY and SOURCE_ARTIFACT_KEY ...
        existingModuleDir = ResourceUtil.resourceFile("repositories/module/target");

        // ... except for the binary files
        generateBinaryTestFile(new File(existingModuleDir, "the-bundle.jar"), BUNDLE_ARTIFACT_SIZE);
        generateBinaryTestFile(new File(existingModuleDir, "the-sources.jar"), SOURCE_ARTIFACT_SIZE);
    }

    @Test
    public void testLoadRepository() throws Exception {
        subject = ModuleArtifactRepository.restoreInstance(null, existingModuleDir);

        assertThat(artifactSizeOf(BUNDLE_ARTIFACT_KEY, subject), is(BUNDLE_ARTIFACT_SIZE));
        assertThat(artifactSizeOf(SOURCE_ARTIFACT_KEY, subject), is(SOURCE_ARTIFACT_SIZE));
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        subject = (ModuleArtifactRepository) loadRepositoryViaAgent(existingModuleDir);

        assertThat(subject.getArtifactDescriptors(SOURCE_ARTIFACT_KEY).length, is(1));
    }

    @Test
    public void testCreateRepository() throws Exception {
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));

        assertThat(allKeysIn(subject).isEmpty(), is(true));
    }

    @Test
    public void testWriteToRepository() throws Exception {
        subject = ModuleArtifactRepository.createInstance(null, tempManager.newFolder("targetDir"));

        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        assertThat(artifactSizeOf(BINARY_ARTIFACT_KEY, subject), is(BINARY_ARTIFACT_SIZE));
    }

    @Test
    public void testPersistEmptyRepository() throws Exception {
        File repoDir = tempManager.newFolder("targetDir");
        subject = ModuleArtifactRepository.createInstance(null, repoDir);

        IArtifactRepository result = loadRepositoryViaAgent(repoDir);
        assertThat(allKeysIn(result).size(), is(0));
    }

    @Test
    public void testPersistRepository() throws Exception {
        File repoDir = tempManager.newFolder("targetDir");
        subject = ModuleArtifactRepository.createInstance(null, repoDir);

        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        IArtifactRepository result = loadRepositoryViaAgent(repoDir);
        assertThat(artifactSizeOf(BINARY_ARTIFACT_KEY, result), is(BINARY_ARTIFACT_SIZE));
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

    private static void generateBinaryTestFile(File file, int size) throws IOException {
        file.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(file);
        try {
            OutputStream os = new BufferedOutputStream(fos);
            for (int i = 0; i < size; ++i) {
                os.write(0);
            }
            os.flush();
        } finally {
            fos.close();
        }
        file.deleteOnExit();
    }

    static class WriteSessionStub implements WriteSessionContext {

        public ClassifierAndExtension getClassifierAndExtensionForNewKey(IArtifactKey key) {
            return new ClassifierAndExtension(key.getId(), "jar");
        }
    }
}
