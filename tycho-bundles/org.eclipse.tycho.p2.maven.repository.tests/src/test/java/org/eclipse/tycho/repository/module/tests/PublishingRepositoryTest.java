/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module.tests;

import static org.eclipse.tycho.repository.module.tests.ModuleArtifactRepositoryTest.writeAndClose;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.repository.module.ModuleArtifactRepository;
import org.eclipse.tycho.repository.module.ModuleMetadataRepository;
import org.eclipse.tycho.repository.module.PublishingRepositoryView;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class PublishingRepositoryTest {

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private PublishingRepositoryView subject;

    @Test
    public void testAttachedArtifactsMap() throws Exception {
        File targetFolder = tempManager.newFolder("targetDir");
        ModuleMetadataRepository metadataRepo = new ModuleMetadataRepository(null, targetFolder);
        ModuleArtifactRepository artifactRepo = ModuleArtifactRepository.createInstance(null, targetFolder);

        // write one artifact to the repo
        OutputStream outputStream = artifactRepo.getOutputStream(artifactRepo.createArtifactDescriptor(
                AttachedTestArtifact.key, AttachedTestArtifact.getWriteSessionForArtifact()));
        writeAndClose(outputStream, AttachedTestArtifact.size);

        subject = new PublishingRepositoryView(metadataRepo, artifactRepo);

        Map<String, File> artifacts = subject.getArtifactLocations();
        assertThat(artifacts.keySet(), hasItem(AttachedTestArtifact.classifier));
        assertThat(artifacts.keySet(), hasItem("p2metadata"));
        assertThat(artifacts.keySet(), hasItem("p2artifacts"));

        for (File artifactFile : artifacts.values()) {
            assertThat(artifactFile, isFile());
        }
    }

    static class AttachedTestArtifact {
        static final IArtifactKey key = new ArtifactKey("p2classifier", "id", Version.parseVersion("0.1.2"));
        static final String classifier = "mvnclassifier";
        static final int size = 6;

        public static WriteSessionContext getWriteSessionForArtifact() {
            return new WriteSessionContext() {

                public String getClassifierForNewKey(IArtifactKey newKey) {
                    assertSame(key, newKey);
                    return classifier;
                }
            };
        }
    }

}
