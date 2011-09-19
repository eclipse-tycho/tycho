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
import java.net.URI;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class LocalArtifactRepositoryTest {

    private final File basedir = new File("target/repository").getAbsoluteFile();

    @Before
    public void cleanupRepository() {
        deleteDir(basedir);
    }

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
    public void getP2Location() {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        URI location = repo.getLocation(desc);
        Assert.assertEquals(new File(basedir,
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0.jar").toURI(), location);

        ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] { new ProcessingStepDescriptor(
                "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) };
        desc.setProcessingSteps(steps);
        desc.setProperty(IArtifactDescriptor.FORMAT, "packed");

        location = repo.getLocation(desc);
        Assert.assertEquals(new File(basedir,
                "p2/osgi/bundle/org.eclipse.tycho.test.p2/1.0.0/org.eclipse.tycho.test.p2-1.0.0-pack200.jar.pack.gz")
                .toURI(), location);
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
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        URI location = repo.getLocation(desc);
        Assert.assertEquals(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar").toURI(), location);
    }

    @Test
    public void getMavenLocationWithClassifierAndExtension() {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        Assert.assertEquals(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar").toURI(), repo
                .getLocation(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, "classifier.value");
        Assert.assertEquals(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.jar")
                .toURI(), repo.getLocation(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_EXTENSION, "zip");
        Assert.assertEquals(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0-classifier.value.zip")
                .toURI(), repo.getLocation(desc));

        desc.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, null);
        Assert.assertEquals(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.zip").toURI(), repo
                .getLocation(desc));
    }

    @Test
    public void addP2Artifact() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, desc);

        Assert.assertTrue(new File(basedir,
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
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);

        ArtifactDescriptor desc = newBundleArtifactDescriptor(true);

        writeDummyArtifact(repo, desc);

        Assert.assertTrue(new File(basedir,
                "group/org.eclipse.tycho.test.maven/1.0.0/org.eclipse.tycho.test.maven-1.0.0.jar").exists());
        Assert.assertTrue(repo.contains(desc.getArtifactKey()));
        Assert.assertTrue(repo.contains(desc));
    }

    @Test
    public void reload() throws Exception {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
        ArtifactDescriptor mavenArtifact = newBundleArtifactDescriptor(true);
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);

        writeDummyArtifact(repo, mavenArtifact);
        writeDummyArtifact(repo, p2Artifact);

        repo.save();

        repo = new LocalArtifactRepository(basedir);
        Assert.assertTrue(repo.contains(mavenArtifact.getArtifactKey()));
        Assert.assertTrue(repo.contains(p2Artifact.getArtifactKey()));
    }

    @Test
    public void testGetArtifactsNoRequests() {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
        IStatus status = repo.getArtifacts(new IArtifactRequest[0], new NullProgressMonitor());
        Assert.assertTrue(status.isOK());
    }

    @Test
    public void testGetArtifactsErrorRequest() {
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
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
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
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
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor(false);
        byte[] content = new byte[] { 111, 112 };
        writeDummyArtifact(repo, p2Artifact, content);
        Assert.assertTrue(repo.contains(p2Artifact.getArtifactKey()));
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        repo.getRawArtifact(p2Artifact, destination, new NullProgressMonitor());
        Assert.assertArrayEquals(content, destination.toByteArray());
    }

}
