/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module.tests;

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
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.tests.Activator;
import org.eclipse.tycho.repository.module.ModuleArtifactRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
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

    private ModuleArtifactRepository subject;

    @BeforeClass
    public static void initExistingRepository() throws Exception {
        // this folder contains a ModuleArtifactRepository with BUNDLE_ARTIFACT_KEY and SOURCE_ARTIFACT_KEY ...
        existingModuleDir = new File("resources/repositories/module/target").getAbsoluteFile();

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
        File agentDir = tempManager.newFolder("agent");
        IProvisioningAgent agent = Activator.createProvisioningAgent(agentDir.toURI());
        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        subject = (ModuleArtifactRepository) repoManager.loadRepository(existingModuleDir.toURI(), null);

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

        IArtifactRepository result = reloadRepository(repoDir);
        assertThat(allKeysIn(result).size(), is(0));
    }

    @Test
    public void testPersistRepository() throws Exception {
        File repoDir = tempManager.newFolder("targetDir");
        subject = ModuleArtifactRepository.createInstance(null, repoDir);

        OutputStream outputStream = subject.getOutputStream(newDescriptor(BINARY_ARTIFACT_KEY));
        writeAndClose(outputStream, BINARY_ARTIFACT_SIZE);

        IArtifactRepository result = reloadRepository(repoDir);
        assertThat(artifactSizeOf(BINARY_ARTIFACT_KEY, result), is(BINARY_ARTIFACT_SIZE));
    }

    private IArtifactDescriptor newDescriptor(ArtifactKey artifactKey) {
        return subject.createArtifactDescriptor(artifactKey, new WriteSessionStub());
    }

    private static Set<IArtifactKey> allKeysIn(IArtifactRepository subject) {
        IQueryResult<IArtifactKey> queryResult = subject.query(new ExpressionMatchQuery<IArtifactKey>(
                IArtifactKey.class, ExpressionUtil.TRUE_EXPRESSION), null);
        return queryResult.toUnmodifiableSet();
    }

    private static int artifactSizeOf(IArtifactKey artifactKey, IArtifactRepository subject) {
        IArtifactDescriptor[] artifactDescriptors = subject.getArtifactDescriptors(artifactKey);
        assertEquals(1, artifactDescriptors.length);

        ByteArrayOutputStream artifactContent = new ByteArrayOutputStream();
        subject.getArtifact(artifactDescriptors[0], artifactContent, null);
        return artifactContent.size();
    }

    private IArtifactRepository reloadRepository(File location) throws Exception {
        // load through factory, to ensure that end-to-end process works
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempManager.newFolder("agent").toURI());
        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

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

        public String getClassifierForNewKey(IArtifactKey key) {
            return key.getId();
        }
    }
}
