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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ReactorProjectCoordinatesStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class PublishingRepositoryTest {

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();

    private PublishingRepository subject;

    @Before
    public void initSubject() throws Exception {
        ReactorProjectCoordinates project = new ReactorProjectCoordinatesStub(tempManager.newFolder("targetDir"));

        subject = new PublishingRepositoryImpl(p2Context.getAgent(), project);
    }

    @Test
    public void testArtifactsMap() throws Exception {
        // simulate that AttachedTestArtifact is the build output
        insertTestArtifact(subject);

        Map<String, File> artifacts = subject.getArtifactLocations();
        assertThat(artifacts.keySet(), hasItem(AttachedTestArtifact.classifier));
        assertThat(artifacts.keySet(), hasItem("p2metadata"));
        assertThat(artifacts.keySet(), hasItem("p2artifacts"));

        for (File artifactFile : artifacts.values()) {
            assertThat(artifactFile, isFile());
        }
    }

    @Test
    public void testArtifactsMapWithOnlyMetafiles() throws Exception {
        // although there is no published content, the meta-files shall still be there
        Map<String, File> artifacts = subject.getArtifactLocations();
        assertThat(artifacts.keySet(), hasItem("p2metadata"));
        assertThat(artifacts.keySet(), hasItem("p2artifacts"));

        for (File artifactFile : artifacts.values()) {
            assertThat(artifactFile, isFile());
        }
    }

    private static void insertTestArtifact(PublishingRepository publishingRepo) throws ProvisionException, IOException {
        IArtifactRepository writableArtifactRepo = publishingRepo.getArtifactRepositoryForWriting(AttachedTestArtifact
                .getWriteSessionForArtifact());
        OutputStream outputStream = writableArtifactRepo.getOutputStream(writableArtifactRepo
                .createArtifactDescriptor(AttachedTestArtifact.key));
        writeAndClose(outputStream, AttachedTestArtifact.size);
    }

    private static class AttachedTestArtifact {
        static final IArtifactKey key = new ArtifactKey("p2classifier", "id", Version.parseVersion("0.1.2"));
        static final String classifier = "mvnclassifier";
        static final int size = 6;

        static WriteSessionContext getWriteSessionForArtifact() {
            return new WriteSessionContext() {

                public String getClassifierForNewKey(IArtifactKey newKey) {
                    assertSame(key, newKey);
                    return classifier;
                }
            };
        }
    }

}
