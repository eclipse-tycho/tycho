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
package org.eclipse.tycho.p2.maven.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalArtifactRepository extends AbstractMavenArtifactRepository {

    private final Set<IArtifactKey> changedDescriptors = new HashSet<IArtifactKey>();

    public LocalArtifactRepository(File location) {
        this(Activator.getProvisioningAgent(), location);
    }

    public LocalArtifactRepository(IProvisioningAgent agent, File location) {
        super(agent, location.toURI(), new LocalTychoRepositoryIndex(location,
                LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH), new LocalRepositoryReader(location));
    }

    public LocalArtifactRepository(File location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator) {
        super(Activator.getProvisioningAgent(), location.toURI(), projectIndex, contentLocator);
    }

    private void saveMaven() {
        File location = getBasedir();

        LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex(location,
                LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);

        ArtifactsIO io = new ArtifactsIO();

        for (IArtifactKey key : changedDescriptors) {
            Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
            if (keyDescriptors != null && !keyDescriptors.isEmpty()) {
                IArtifactDescriptor random = keyDescriptors.iterator().next();
                GAV gav = RepositoryLayoutHelper.getGAV(random.getProperties());

                if (gav == null) {
                    gav = getP2GAV(random);
                }

                index.addProject(gav);

                String relpath = getMetadataRelpath(gav);

                File file = new File(location, relpath);
                file.getParentFile().mkdirs();

                try {
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        io.writeXML(keyDescriptors, os);
                    } finally {
                        os.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            index.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        changedDescriptors.clear();
    }

    private String getMetadataRelpath(GAV gav) {
        String relpath = RepositoryLayoutHelper.getRelativePath(gav, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
        return relpath;
    }

    public void save() {
        saveMaven();
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        // we believe we do not need to implement artifact pre and post processing as in the
        // org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository
        // because the P2 mirroring has already done that
        return getRawArtifact(descriptor, destination, monitor);
    }

    @Override
    public synchronized OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        GAV gav = RepositoryLayoutHelper.getGAV(descriptor.getProperties());

        if (gav == null) {
            gav = getP2GAV(descriptor);
        }

        File basedir = getBasedir();
        File file = new File(basedir, RepositoryLayoutHelper.getRelativePath(gav, null, null));
        file.getParentFile().mkdirs();

        // TODO ideally, repository index should be updated after artifact has been written to the file

        ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
        newDescriptor.setRepository(this);
        descriptors.add(newDescriptor);

        IArtifactKey key = newDescriptor.getArtifactKey();
        Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
        if (keyDescriptors == null) {
            keyDescriptors = new HashSet<IArtifactDescriptor>();
            descriptorsMap.put(key, keyDescriptors);
        }
        keyDescriptors.add(newDescriptor);

        changedDescriptors.add(key);

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new ProvisionException("Could not create artifact file", e);
        }
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        URI location = getLocation(descriptor);
        try {
            InputStream source = location.toURL().openStream();
            try {
                copyStream(source, destination);
            } finally {
                source.close();
            }
        } catch (MalformedURLException e) {
            return new Status(IStatus.ERROR, Activator.ID, "Invalid location in artifact descriptor: " + descriptor, e);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.ID, "Could not retrieve artifact from location: " + location, e);
        }
        return Status.OK_STATUS;
    }

    private static void copyStream(final InputStream source, final OutputStream destination) throws IOException {
        final byte[] buffer = new byte[8192];
        int length;
        while ((length = source.read(buffer)) != -1) {
            destination.write(buffer, 0, length);
        }
    }

    public File getBasedir() {
        return new File(getLocation());
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

    public URI getLocation(IArtifactDescriptor descriptor) {
        return getLocationFile(descriptor).toURI();
    }

    private File getLocationFile(IArtifactDescriptor descriptor) {
        GAV gav = getGAV(descriptor);

        File basedir = getBasedir();

        String classifier = descriptor.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
        String extension = descriptor.getProperty(RepositoryLayoutHelper.PROP_EXTENSION);

        if ("packed".equals(descriptor.getProperty(IArtifactDescriptor.FORMAT))) {
            classifier = "pack200";
            extension = "jar.pack.gz";
        }

        return new File(basedir, RepositoryLayoutHelper.getRelativePath(gav, classifier, extension));
    }

    @Override
    public IStatus resolve(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        return super.contains(descriptor) && getLocationFile(descriptor).canRead();
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor) {
        super.addDescriptor(descriptor);

        changedDescriptors.add(descriptor.getArtifactKey());
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor) {
        super.removeDescriptor(descriptor);

        IArtifactKey key = descriptor.getArtifactKey();

        Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);

        if (keyDescriptors != null) {
            keyDescriptors.remove(descriptor);
            if (keyDescriptors.isEmpty()) {
                descriptorsMap.remove(key);
            }
        }

        descriptors.remove(descriptor);
        getLocationFile(descriptor).delete();

        changedDescriptors.remove(descriptor.getArtifactKey());
    }

}
