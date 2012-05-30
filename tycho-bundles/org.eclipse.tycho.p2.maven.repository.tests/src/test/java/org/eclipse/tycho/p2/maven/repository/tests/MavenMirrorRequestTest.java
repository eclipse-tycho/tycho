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

import java.io.File;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.MavenMirrorRequest;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("restriction")
public class MavenMirrorRequestTest extends BaseMavenRepositoryTest {
    private IProgressMonitor monitor = new NullProgressMonitor();

    @Test
    public void testMirror() throws Exception {
        IProvisioningAgent agent = Activator.getProvisioningAgent();
        Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);

        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository repository = manager.loadRepository(new File("resources/repositories/e342").toURI(),
                monitor);

        LocalArtifactRepository localRepository = new LocalArtifactRepository(localRepoIndices);

        IArtifactKey key = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
                Version.parseVersion("3.4.3.R34x_v20081215-1030"));

        MavenMirrorRequest request = new MavenMirrorRequest(key, localRepository, transport, false);

        repository.getArtifacts(new IArtifactRequest[] { request }, monitor);

        IArtifactDescriptor[] descriptors = localRepository.getArtifactDescriptors(key);
        Assert.assertEquals(1, descriptors.length);
        Assert.assertNotNull(descriptors[0]);
        Assert.assertTrue(descriptors[0].getProperties().isEmpty());
        Assert.assertEquals(0, descriptors[0].getProcessingSteps().length);
    }

    @Test
    public void testMirrorNoCanonicalArtifact() throws Exception {
        IProvisioningAgent agent = Activator.getProvisioningAgent();
        Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);

        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository repository = manager.loadRepository(new File("resources/repositories/packgz").toURI(),
                monitor);

        LocalArtifactRepository localRepository = new LocalArtifactRepository(localRepoIndices);

        IArtifactKey key = new ArtifactKey("osgi.bundle", "org.eclipse.ecf",
                Version.parseVersion("3.1.300.v20120319-0616"));

        MavenMirrorRequest request = new MavenMirrorRequest(key, localRepository, transport, true);

        repository.getArtifacts(new IArtifactRequest[] { request }, monitor);

        IArtifactDescriptor[] descriptors = localRepository.getArtifactDescriptors(key);
        Assert.assertEquals(2, descriptors.length);
        IArtifactDescriptor packed = getDescriptor(descriptors, IArtifactDescriptor.FORMAT_PACKED);
        Assert.assertNotNull(packed);
        Assert.assertEquals(IArtifactDescriptor.FORMAT_PACKED, packed.getProperty(IArtifactDescriptor.FORMAT));
        Assert.assertEquals(1, packed.getProcessingSteps().length);
        Assert.assertEquals("org.eclipse.equinox.p2.processing.Pack200Unpacker",
                packed.getProcessingSteps()[0].getProcessorId());

        IArtifactDescriptor canonical = getDescriptor(descriptors, null);
        Assert.assertNotNull(canonical);
        Assert.assertTrue(canonical.getProperties().isEmpty());
        Assert.assertEquals(0, canonical.getProcessingSteps().length);
        File file = localRepository.getArtifactFile(canonical);
        Assert.assertTrue(file.isFile() && file.canRead());
        // make sure this is actually a jar
        JarFile jar = new JarFile(file);
        try {
            Assert.assertNotNull(jar.getManifest());
        } finally {
            jar.close();
        }

    }

    @Test
    public void testCorruptedPackedArtifact() throws Exception {
        IProvisioningAgent agent = Activator.getProvisioningAgent();
        Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);

        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository repository = manager.loadRepository(new File("resources/repositories/packgz").toURI(),
                monitor);

        LocalArtifactRepository localRepository = new LocalArtifactRepository(localRepoIndices);

        IArtifactKey key = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
                Version.parseVersion("3.4.3.R34x_v20081215-1030"));

        MavenMirrorRequest request = new MavenMirrorRequest(key, localRepository, transport, true);

        repository.getArtifacts(new IArtifactRequest[] { request }, monitor);

        IArtifactDescriptor[] descriptors = localRepository.getArtifactDescriptors(key);
        Assert.assertEquals(2, descriptors.length);
        Assert.assertNotNull(getDescriptor(descriptors, null)); // canonical
        Assert.assertNotNull(getDescriptor(descriptors, IArtifactDescriptor.FORMAT_PACKED));

        File file = localRepository.getArtifactFile(getDescriptor(descriptors, null));
        Assert.assertTrue(file.isFile() && file.canRead());

        // make sure this is actually a jar
        JarFile jar = new JarFile(file);
        try {
            Assert.assertNotNull(jar.getManifest());
        } finally {
            jar.close();
        }
    }

    @Test
    public void testArtifactImplementationClass() throws Exception {
        // The point of this test is to ensure that the same IArtifact implementation used by MavenMirrorRequest
        // and by (maven) LocalArtifactRepository. If MavenMirrorRequest and LocalArtifactRepository use different
        // IArtifact implementation, this results in duplicate copies of the artifact descriptor.

        IProvisioningAgent agent = Activator.getProvisioningAgent();
        Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);

        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository repository = manager.loadRepository(new File("resources/repositories/e342").toURI(),
                monitor);

        IArtifactKey key = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
                Version.parseVersion("3.4.3.R34x_v20081215-1030"));

        LocalArtifactRepository localRepository;
        MavenMirrorRequest request;
        IArtifactDescriptor[] descriptors;

        localRepository = new LocalArtifactRepository(localRepoIndices);

        // mirror
        request = new MavenMirrorRequest(key, localRepository, transport, false);
        repository.getArtifacts(new IArtifactRequest[] { request }, monitor);
        descriptors = localRepository.getArtifactDescriptors(key);
        Assert.assertEquals(1, descriptors.length);
        Assert.assertTrue(descriptors[0].getClass() == ArtifactDescriptor.class);

        // force reload
        localRepository.save();

        // make sure the same artifact implementation is loaded from metadata
        localRepository = new LocalArtifactRepository(localRepoIndices);
        descriptors = localRepository.getArtifactDescriptors(key);
        Assert.assertEquals(1, descriptors.length);
        Assert.assertTrue(descriptors[0].getClass() == ArtifactDescriptor.class);
    }

    private IArtifactDescriptor getDescriptor(IArtifactDescriptor[] descriptors, String format) {
        if (descriptors == null) {
            return null;
        }

        for (IArtifactDescriptor descriptor : descriptors) {
            if (format != null) {
                if (format.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT))) {
                    return descriptor;
                }
            } else if (descriptor.getProperty(IArtifactDescriptor.FORMAT) == null) {
                return descriptor;
            }
        }

        return null;
    }
}
