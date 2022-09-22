/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.resolver.FileTargetDefinitionContent;

public final class FileArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

    private Supplier<Iterator<IArtifactDescriptor>> descriptorSupplier;

    public FileArtifactRepository(IProvisioningAgent agent,
            Supplier<Iterator<IArtifactDescriptor>> descriptorSupplier) {
        super(agent, null, null, null, null, null, null, null);
        this.descriptorSupplier = descriptorSupplier;
    }

    @Override
    public synchronized void setLocation(URI location) {
        super.setLocation(location);
    }

    @Override
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        File artifactFile = getArtifactFile(descriptor);
        if (artifactFile == null) {
            return new Status(IStatus.ERROR, FileTargetDefinitionContent.class.getName(), "Artifact not found");
        }
        try {
            if (artifactFile.isDirectory()) {
                File manifestFile = new File(artifactFile, JarFile.MANIFEST_NAME);
                try (JarOutputStream jarOutputStream = new JarOutputStream(destination)) {
                    if (manifestFile.exists()) {
                        Manifest manifest = new Manifest(new FileInputStream(manifestFile));
                        manifest.getMainAttributes().putValue("Eclipse-BundleShape", "dir");
                        jarOutputStream.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
                        manifest.write(jarOutputStream);
                        jarOutputStream.closeEntry();
                    }
                    copyToStream(artifactFile, jarOutputStream, null, f -> !f.equals(manifestFile));
                }
            } else {
                try (FileInputStream inputStream = new FileInputStream(artifactFile)) {
                    inputStream.transferTo(destination);
                }
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, FileTargetDefinitionContent.class.getName(), "transfer failed", e);
        }
        return Status.OK_STATUS;
    }

    private void copyToStream(File file, ZipOutputStream os, String path, FileFilter fileFilter) throws IOException {
        if (file.isFile()) {
            try (FileInputStream is = new FileInputStream(file)) {
                ZipEntry entry = new ZipEntry(path == null ? file.getName() : path + file.getName());
                entry.setLastModifiedTime(FileTime.fromMillis(file.lastModified()));
                os.putNextEntry(entry);
                is.transferTo(os);
                os.closeEntry();
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles(fileFilter);
            if (files != null && files.length > 0) {
                for (File file2 : files) {
                    copyToStream(file2, os, path == null ? "" : path + file.getName() + "/", fileFilter);
                }
            }
        } else {
            throw new IOException(
                    "file " + file.getAbsolutePath() + " is neither a readable file nor a readable directory");
        }
    }

    @Override
    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return (query, monitor) -> query.perform(descriptorSupplier.get());
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
        return query.perform(new Iterator<IArtifactKey>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public IArtifactKey next() {
                IArtifactDescriptor next = iterator.next();
                return next.getArtifactKey();
            }
        });
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
        while (iterator.hasNext()) {
            IArtifactDescriptor thisArtifactDescriptor = iterator.next();
            if (thisArtifactDescriptor.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(IArtifactKey key) {
        Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
        while (iterator.hasNext()) {
            IArtifactDescriptor descriptor = iterator.next();
            if (matches(key, descriptor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return getRawArtifact(descriptor, destination, monitor);
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
        while (iterator.hasNext()) {
            IArtifactDescriptor descriptor = iterator.next();
            if (matches(key, descriptor)) {
                return new IArtifactDescriptor[] { descriptor };
            }
        }
        return new IArtifactDescriptor[0];
    }

    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        SubMonitor convert = SubMonitor.convert(monitor, requests.length);
        MultiStatus multiStatus = new MultiStatus(FileTargetDefinitionContent.class.getName(), IStatus.INFO,
                "Request Status");
        boolean ok = true;
        for (IArtifactRequest request : requests) {
            request.perform(this, convert.split(1));
            IStatus result = request.getResult();
            multiStatus.add(result);
            ok &= result.isOK();
        }
        return ok ? Status.OK_STATUS : multiStatus;
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new ProvisionException("read only");
    }

    @Override
    public File getArtifactFile(IArtifactKey key) {
        Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
        while (iterator.hasNext()) {
            IArtifactDescriptor descriptor = iterator.next();
            if (matches(key, descriptor)) {
                return getArtifactFile(descriptor);
            }
        }
        return null;
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return descriptor instanceof FileArtifactDescriptor fileArtifactDescriptor //
                ? fileArtifactDescriptor.file
                : null;
    }

    private static boolean matches(IArtifactKey key, IArtifactDescriptor descriptor) {
        IArtifactKey descriptorKey = descriptor.getArtifactKey();
        return descriptorKey == key || (key.getId().equals(descriptorKey.getId())
                && key.getClassifier().equals(descriptorKey.getClassifier())
                && key.getVersion().equals(descriptorKey.getVersion()));
    }

    public static IArtifactDescriptor forFile(File file, IArtifactKey key) {
        return new FileArtifactDescriptor(file, key);
    }

    private static final class FileArtifactDescriptor extends ArtifactDescriptor {

        private File file;

        private FileArtifactDescriptor(File file, IArtifactKey key) {
            super(key);
            this.file = file;
        }

    }

}
