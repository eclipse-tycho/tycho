/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.ModuleArtifactRepository;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ModuleArtifactRepositoryTest {

    private static final IArtifactKey BUNDLE_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "bundle",
            Version.parseVersion("1.2.3.201011101425"));

    private static final int BUNDLE_ARTIFACT_SIZE = 1841;

    private static final IArtifactKey SOURCE_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "bundle.source",
            Version.parseVersion("1.2.3.TAGNAME"));

    private static final int SOURCE_ARTIFACT_SIZE = 418;

    private static File moduleDir;

    private File tempDir = null;

    private ModuleArtifactRepository subject;

    @BeforeClass
    public static void init() throws Exception {
        moduleDir = new File("resources/repositories/module/target").getAbsoluteFile();

        generateBinaryTestFile(new File(moduleDir, "the-bundle.jar"), BUNDLE_ARTIFACT_SIZE);
        generateBinaryTestFile(new File(moduleDir, "the-sources.jar"), SOURCE_ARTIFACT_SIZE);
    }

    @After
    public void cleanUp() {
        if (tempDir != null)
            FileUtils.deleteAll(tempDir);
        tempDir = null;
    }

    @Test
    public void testLoadRepository() throws Exception {
        subject = new ModuleArtifactRepository(null, moduleDir);

        assertGetArtifact(subject, BUNDLE_ARTIFACT_KEY, BUNDLE_ARTIFACT_SIZE);
        assertGetArtifact(subject, SOURCE_ARTIFACT_KEY, SOURCE_ARTIFACT_SIZE);
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        tempDir = createTempDir();
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempDir.toURI());
        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository subject = repoManager.loadRepository(moduleDir.toURI(), null);

        assertEquals(subject.getArtifactDescriptors(SOURCE_ARTIFACT_KEY).length, 1);
    }

    private static void assertGetArtifact(IArtifactRepository subject, IArtifactKey artifactKey, int expectedSize) {
        IArtifactDescriptor[] artifactDescriptors = subject.getArtifactDescriptors(artifactKey);
        assertEquals(1, artifactDescriptors.length);

        ByteArrayOutputStream artifactContent = new ByteArrayOutputStream();
        subject.getArtifact(artifactDescriptors[0], artifactContent, null);
        assertEquals(expectedSize, artifactContent.size());
    }

    private static void generateBinaryTestFile(File file, int size) throws FileNotFoundException, IOException {
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

    private static File createTempDir() throws IOException {
        return createTempDir(ModuleArtifactRepositoryTest.class);
    }

    static File createTempDir(Class<?> testClass) throws IOException {
        final File tempFile = File.createTempFile(testClass.getSimpleName(), "");
        tempFile.delete();

        final File tempDir = tempFile;
        if (!tempDir.mkdirs())
            throw new IOException("Could not create temporary directory: " + tempDir);
        return tempFile;
    }
}
