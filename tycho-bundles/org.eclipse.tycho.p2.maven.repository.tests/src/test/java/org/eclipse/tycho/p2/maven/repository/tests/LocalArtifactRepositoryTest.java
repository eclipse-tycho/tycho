/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

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
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("restriction")
public class LocalArtifactRepositoryTest extends BaseMavenRepositoryTest {

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

    @Test
    public void testOutdatedIndex() {
        //create Repo with single artifact
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);
        repo.addDescriptor(desc);
        repo.save();

        // check: the artifact is in the index
        TychoRepositoryIndex artifactIndex = createArtifactsIndex(baseDir);
        Assert.assertFalse(artifactIndex.getProjectGAVs().isEmpty());

        // delete artifact content from file system
        deleteDir(new File(baseDir, "p2"));

        // create a new repo and check that the reference was gracefully removed from the index
        repo = new LocalArtifactRepository(localRepoIndices);
        repo.save();
        artifactIndex = createArtifactsIndex(baseDir);
        Assert.assertTrue(artifactIndex.getProjectGAVs().isEmpty());
    }

    @Test
    public void getP2Location() {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        Assert.assertEquals(new File(baseDir,
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar"), repo
                .getArtifactFile(desc));

        ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] { new ProcessingStepDescriptor(
                "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) };
        desc.setProcessingSteps(steps);
        desc.setProperty(IArtifactDescriptor.FORMAT, "packed");

        Assert.assertEquals(new File(baseDir,
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0-pack200.jar.pack.gz"),
                repo.getArtifactFile(desc));
    }

    private ArtifactDescriptor newBundleArtifactDescriptor(boolean maven) {
        ArtifactKey key = new ArtifactKey(PublisherHelper.OSGI_BUNDLE_CLASSIFIER, "org.eclipse.tycho.test."
                + (maven ? "maven" : "p2"), Version.createOSGi(1, 0, 0));
        ArtifactDescriptor desc = new ArtifactDescriptor(key);

        if (maven) {
            desc.setProperty(RepositoryLayoutHelper.PROP_GROUP_ID, "group");
            desc.setProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID, key.getId());
            desc.setProperty(RepositoryLayoutHelper.PROP_VERSION, key.getVersion().toString());
        }

        return desc;
    }

    @Test
    public void getMavenLocation() {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        Assert.assertEquals(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar"), repo
                .getArtifactFile(desc));
    }

    @Test
    public void getMavenLocationWithClassifierAndExtension() {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        Assert.assertEquals(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar"), repo
                .getArtifactFile(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, "classifier.value");
        Assert.assertEquals(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.jar"),
                repo.getArtifactFile(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_EXTENSION, "zip");
        Assert.assertEquals(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.zip"),
                repo.getArtifactFile(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, null);
        Assert.assertEquals(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.zip"), repo
                .getArtifactFile(desc));
    }

    @Test
    public void addP2Artifact() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, desc);

        Assert.assertTrue(new File(baseDir,
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar").exists());
        Assert.assertTrue(repo.contains(desc.getArtifactKey()));
        Assert.assertTrue(repo.contains(desc));
    }

    private void writeDummyArtifact(LocalArtifactRepository repo, ArtifactDescriptor desc) throws ProvisionException,
            IOException {
        writeDummyArtifact(repo, desc, new byte[] { 111 });
    }

    private void writeDummyArtifact(LocalArtifactRepository repo, ArtifactDescriptor desc, byte[] content)
            throws ProvisionException, IOException {
        OutputStream os = repo.getOutputStream(desc);
        os.write(content);
        os.close();
    }

    @Test
    public void addMavenArtifact() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        writeDummyArtifact(repo, desc);

        Assert.assertTrue(new File(baseDir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar").exists());
        Assert.assertTrue(repo.contains(desc.getArtifactKey()));
        Assert.assertTrue(repo.contains(desc));
    }

    @Test
    public void reload() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        ArtifactDescriptor mavenArtifact = newBundleArtifactDescriptor(true);
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, mavenArtifact);
        writeDummyArtifact(repo, p2Artifact);

        repo.save();

        repo = new LocalArtifactRepository(localRepoIndices);
        Assert.assertTrue(repo.contains(mavenArtifact.getArtifactKey()));
        Assert.assertTrue(repo.contains(p2Artifact.getArtifactKey()));
    }

    @Test
    public void testGetArtifactsNoRequests() {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        IStatus status = repo.getArtifacts(new IArtifactRequest[0], new NullProgressMonitor());
        Assert.assertTrue(status.isOK());
    }

    @Test
    public void testGetArtifactsErrorRequest() {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        IArtifactRequest errorRequest = new IArtifactRequest() {
            public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
            }

            public IStatus getResult() {
                return new Status(IStatus.ERROR, Activator.ID, "Error");
            }

            public IArtifactKey getArtifactKey() {
                return null;
            }
        };
        IStatus status = repo.getArtifacts(new IArtifactRequest[] { errorRequest }, new NullProgressMonitor());
        Assert.assertFalse(status.isOK());
    }

    @Test
    public void testGetArtifactsCreateSubmonitor() {
        NullProgressMonitor monitor = new NullProgressMonitor();
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        final IProgressMonitor[] requestMonitorReturnValue = new IProgressMonitor[1];
        IArtifactRequest errorRequest = new IArtifactRequest() {
            public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
                requestMonitorReturnValue[0] = monitor;
            }

            public IStatus getResult() {
                return Status.OK_STATUS;
            }

            public IArtifactKey getArtifactKey() {
                return null;
            }
        };
        IStatus status = repo.getArtifacts(new IArtifactRequest[] { errorRequest }, monitor);
        Assert.assertTrue(status.isOK());
        Assert.assertNotNull(requestMonitorReturnValue[0]);
        Assert.assertNotSame(monitor, requestMonitorReturnValue[0]);
    }

    @Test
    public void testGetRawArtifactDummy() throws ProvisionException, IOException {
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);
        byte[] content = new byte[] { 111, 112 };
        writeDummyArtifact(repo, p2Artifact, content);
        Assert.assertTrue(repo.contains(p2Artifact.getArtifactKey()));
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        repo.getRawArtifact(p2Artifact, destination, new NullProgressMonitor());
        Assert.assertArrayEquals(content, destination.toByteArray());
    }

}
