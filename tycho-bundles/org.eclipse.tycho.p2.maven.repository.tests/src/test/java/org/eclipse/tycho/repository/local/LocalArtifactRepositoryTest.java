/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.repository.local.index.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("restriction")
public class LocalArtifactRepositoryTest {

    @Rule
    public TemporaryLocalMavenRepository mvnRepo = new TemporaryLocalMavenRepository();

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);

                }
                file.delete();
            }
        }
    }

    private TychoRepositoryIndex createArtifactsIndex(File location) {
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location, new NoopFileLockService(),
                new MockMavenContext(location, mock(MavenLogger.class)));
    }

    @Test
    public void testOutdatedIndex() {
        //create Repo with single artifact
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);
        repo.addDescriptor(desc);
        repo.save();

        // check: the artifact is in the index
        TychoRepositoryIndex artifactIndex = createArtifactsIndex(mvnRepo.getLocalRepositoryRoot());
        Assert.assertFalse(artifactIndex.getProjectGAVs().isEmpty());

        // delete artifact content from file system
        deleteDir(new File(mvnRepo.getLocalRepositoryRoot(), "p2"));

        // create a new repo and check that the reference was gracefully removed from the index
        repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        repo.save();
        artifactIndex = createArtifactsIndex(mvnRepo.getLocalRepositoryRoot());
        assertTrue(artifactIndex.getProjectGAVs().isEmpty());
    }

    @Test
    public void getP2Location() {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        assertEquals(
                new File(mvnRepo.getLocalRepositoryRoot(),
                        "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar"),
                repo.internalGetArtifactStorageLocation(desc));

        assertEquals(
                new File(mvnRepo.getLocalRepositoryRoot(),
                        "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar"),
                repo.internalGetArtifactStorageLocation(desc));
    }

    private ArtifactDescriptor newBundleArtifactDescriptor(boolean maven) {
        ArtifactKey key = new ArtifactKey(PublisherHelper.OSGI_BUNDLE_CLASSIFIER,
                "org.eclipse.tycho.test." + (maven ? "maven" : "p2"), Version.createOSGi(1, 0, 0));
        ArtifactDescriptor desc = new ArtifactDescriptor(key);

        if (maven) {
            desc.setProperty(TychoConstants.PROP_GROUP_ID, "group");
            desc.setProperty(TychoConstants.PROP_ARTIFACT_ID, key.getId());
            desc.setProperty(TychoConstants.PROP_VERSION, key.getVersion().toString());
        }

        return desc;
    }

    @Test
    public void getMavenLocation() {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        assertEquals(
                new File(mvnRepo.getLocalRepositoryRoot(),
                        "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar"),
                repo.internalGetArtifactStorageLocation(desc));
    }

    @Test
    public void getMavenLocationWithClassifierAndExtension() {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        assertEquals(
                new File(mvnRepo.getLocalRepositoryRoot(),
                        "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar"),
                repo.internalGetArtifactStorageLocation(desc));

        desc.setProperty(TychoConstants.PROP_CLASSIFIER, "classifier.value");
        assertEquals(new File(mvnRepo.getLocalRepositoryRoot(),
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.jar"),
                repo.internalGetArtifactStorageLocation(desc));

        desc.setProperty(TychoConstants.PROP_EXTENSION, "zip");
        assertEquals(new File(mvnRepo.getLocalRepositoryRoot(),
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.zip"),
                repo.internalGetArtifactStorageLocation(desc));

        desc.setProperty(TychoConstants.PROP_CLASSIFIER, null);
        assertEquals(
                new File(mvnRepo.getLocalRepositoryRoot(),
                        "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.zip"),
                repo.internalGetArtifactStorageLocation(desc));
    }

    @Test
    public void addP2Artifact() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, desc);

        assertTrue(new File(mvnRepo.getLocalRepositoryRoot(),
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar").exists());
        assertTrue(repo.contains(desc.getArtifactKey()));
        assertTrue(repo.contains(desc));
    }

    private void writeDummyArtifact(LocalArtifactRepository repo, ArtifactDescriptor desc)
            throws ProvisionException, IOException {
        writeDummyArtifact(repo, desc, new byte[] { 111 });
    }

    private void writeDummyArtifact(LocalArtifactRepository repo, ArtifactDescriptor desc, byte[] content)
            throws ProvisionException, IOException {
        try (OutputStream os = repo.getOutputStream(desc)) {
            os.write(content);
        }
    }

    @Test
    public void addMavenArtifact() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        writeDummyArtifact(repo, desc);

        assertTrue(new File(mvnRepo.getLocalRepositoryRoot(),
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar").exists());
        assertTrue(repo.contains(desc.getArtifactKey()));
        assertTrue(repo.contains(desc));
    }

    @Test
    public void reload() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        ArtifactDescriptor mavenArtifact = newBundleArtifactDescriptor(true);
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, mavenArtifact);
        writeDummyArtifact(repo, p2Artifact);

        repo.save();

        repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        assertTrue(repo.contains(mavenArtifact.getArtifactKey()));
        assertTrue(repo.contains(p2Artifact.getArtifactKey()));
    }

    @Test
    public void testGetArtifactsNoRequests() {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        IStatus status = repo.getArtifacts(new IArtifactRequest[0], new NullProgressMonitor());
        assertTrue(status.isOK());
    }

    @Test
    public void testGetArtifactsErrorRequest() {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        IArtifactRequest errorRequest = new IArtifactRequest() {
            @Override
            public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
            }

            @Override
            public IStatus getResult() {
                return new Status(IStatus.ERROR, "test-bundle", "Error");
            }

            @Override
            public IArtifactKey getArtifactKey() {
                return null;
            }
        };
        IStatus status = repo.getArtifacts(new IArtifactRequest[] { errorRequest }, new NullProgressMonitor());
        assertThat(status, not(okStatus()));
    }

    @Test
    public void testGetRawArtifactDummy() throws ProvisionException, IOException {
        LocalArtifactRepository repo = new LocalArtifactRepository(mvnRepo.getLocalRepositoryIndex());
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);
        byte[] content = new byte[] { 111, 112 };
        writeDummyArtifact(repo, p2Artifact, content);
        assertTrue(repo.contains(p2Artifact.getArtifactKey()));
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        repo.getRawArtifact(p2Artifact, destination, new NullProgressMonitor());
        assertArrayEquals(content, destination.toByteArray());
    }

}
