/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.p2resolver.ModuleArtifactRepositoryTest.writeAndClose;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.allKeysIn;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.repository.publishing.WriteSessionContext.ClassifierAndExtension;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class PublishingRepositoryTest extends TychoPlexusTestCase {

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();

    private PublishingRepository subject;

    // project the publishing repository belongs to
    private ReactorProjectIdentitiesStub project;

    @Before
    public void initSubject() throws Exception {
        project = new ReactorProjectIdentitiesStub(tempManager.newFolder("projectDir"));

        subject = new PublishingRepositoryImpl(lookup(IProvisioningAgent.class), project);
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
            assertTrue(artifactFile.isFile());
        }

        // file name extension is used when attaching the artifacts
        assertThat(artifacts.get(AttachedTestArtifact.classifier).toString(),
                endsWith(AttachedTestArtifact.fileExtension));
        assertThat(artifacts.get("p2metadata").toString(), endsWith(".xml"));
        assertThat(artifacts.get("p2artifacts").toString(), endsWith(".xml"));
    }

    @Test
    public void testArtifactsMapWithOnlyMetafiles() throws Exception {
        // although there is no published content, the meta-files shall still be there
        Map<String, File> artifacts = subject.getArtifactLocations();
        assertThat(artifacts.keySet(), hasItem("p2metadata"));
        assertThat(artifacts.keySet(), hasItem("p2artifacts"));

        for (File artifactFile : artifacts.values()) {
            assertTrue(artifactFile.isFile());
        }
    }

    @Test
    public void testArtifactDescriptor() throws Exception {
        // simulate that AttachedTestArtifact is the build output
        insertTestArtifact(subject);

        IArtifactRepository artifactRepo = subject.getArtifactRepository();
        assertThat(allKeysIn(artifactRepo), hasItem(AttachedTestArtifact.key));

        IArtifactDescriptor[] descriptors = artifactRepo.getArtifactDescriptors(AttachedTestArtifact.key);
        assertEquals(1, descriptors.length);
        Map<String, String> props = descriptors[0].getProperties();
        assertEquals(project.getGroupId(), props.get(TychoConstants.PROP_GROUP_ID));
        assertEquals(project.getArtifactId(), props.get(TychoConstants.PROP_ARTIFACT_ID));
        assertEquals(project.getVersion(), props.get(TychoConstants.PROP_VERSION));
        assertEquals(AttachedTestArtifact.classifier, props.get(TychoConstants.PROP_CLASSIFIER));
    }

    private static void insertTestArtifact(PublishingRepository publishingRepo) throws ProvisionException, IOException {
        IArtifactRepository writableArtifactRepo = publishingRepo
                .getArtifactRepositoryForWriting(AttachedTestArtifact.getWriteSessionForArtifact());
        OutputStream outputStream = writableArtifactRepo
                .getOutputStream(writableArtifactRepo.createArtifactDescriptor(AttachedTestArtifact.key));
        writeAndClose(outputStream, AttachedTestArtifact.size);
    }

    private static class AttachedTestArtifact {
        static final IArtifactKey key = new ArtifactKey("p2classifier", "id", Version.parseVersion("0.1.2"));
        static final String classifier = "mvnclassifier";
        static String fileExtension = "ext";
        static final int size = 6;

        static WriteSessionContext getWriteSessionForArtifact() {
            return newKey -> {
                assertSame(key, newKey);
                return new ClassifierAndExtension(classifier, fileExtension);
            };
        }
    }

}
