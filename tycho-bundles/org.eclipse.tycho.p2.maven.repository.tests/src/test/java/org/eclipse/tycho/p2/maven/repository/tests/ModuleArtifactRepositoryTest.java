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
package org.eclipse.tycho.p2.maven.repository.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.ModuleArtifactRepository;
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

    private static File existingModuleDir;

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private IArtifactRepository subject;

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
        subject = new ModuleArtifactRepository(null, existingModuleDir);

        assertThat(artifactSizeOf(BUNDLE_ARTIFACT_KEY, subject), is(BUNDLE_ARTIFACT_SIZE));
        assertThat(artifactSizeOf(SOURCE_ARTIFACT_KEY, subject), is(SOURCE_ARTIFACT_SIZE));
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        File agentDir = tempManager.newFolder("agent");
        IProvisioningAgent agent = Activator.createProvisioningAgent(agentDir.toURI());
        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        subject = repoManager.loadRepository(existingModuleDir.toURI(), null);

        assertThat(subject.getArtifactDescriptors(SOURCE_ARTIFACT_KEY).length, is(1));
    }

    private static int artifactSizeOf(IArtifactKey artifactKey, IArtifactRepository subject) {
        IArtifactDescriptor[] artifactDescriptors = subject.getArtifactDescriptors(artifactKey);
        assertEquals(1, artifactDescriptors.length);

        ByteArrayOutputStream artifactContent = new ByteArrayOutputStream();
        subject.getArtifact(artifactDescriptors[0], artifactContent, null);
        return artifactContent.size();
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
}
