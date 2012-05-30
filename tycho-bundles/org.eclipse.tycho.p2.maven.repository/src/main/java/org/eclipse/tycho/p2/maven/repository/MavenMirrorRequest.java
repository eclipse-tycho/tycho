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

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

@SuppressWarnings("restriction")
public class MavenMirrorRequest extends MirrorRequest {

    private final boolean includePackedArtifacts;

    public MavenMirrorRequest(IArtifactKey key, LocalArtifactRepository localRepository, Transport transport,
            boolean includePackedArtifacts) {
        super(key, localRepository, null, null, transport);

        this.includePackedArtifacts = includePackedArtifacts;
    }

    @Override
    public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
        setSourceRepository(sourceRepository);

        // igorf
        // the code below is suboptimal. it will download packed artifact twice
        // proper implementation goes beyond how much I know p2 (and far beyond how much I care to learn that nonsense)

        IArtifactDescriptor canonical = null;
        IArtifactDescriptor packed = null;

        for (IArtifactDescriptor descriptor : source.getArtifactDescriptors(getArtifactKey())) {
            if (descriptor.getProperty(IArtifactDescriptor.FORMAT) == null) {
                canonical = descriptor;
            } else if (IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT))) {
                packed = descriptor;
            }
        }

        if (canonical == null) {
            canonical = packed;
        }

        if (canonical == null) {
            // TODO log!
            return;
        }

        if (includePackedArtifacts && packed != null) {
            // raw copy of pack200 artifact+descriptor
            if (!contains(target, packed.getArtifactKey(), true)) {
                monitor.subTask("Downloading packed " + getArtifactKey().getId());
                IStatus status;
                try {
                    OutputStream destination = target.getOutputStream(packed);
                    try {
                        status = source.getRawArtifact(packed, destination, monitor);
                    } finally {
                        try {
                            destination.close();
                        } catch (IOException e) {
                            // ignored
                        }
                    }
                } catch (ProvisionException e) {
                    status = e.getStatus();
                }
                if (!status.isOK()) {
                    if (target.contains(packed)) {
                        target.removeDescriptor(packed, monitor);
                    }
                    setResult(status);
                    return;
                }
            }
        }

        // copy jar
        IArtifactDescriptor targetDescriptor = target.createArtifactDescriptor(canonical.getArtifactKey());
        IStatus status = Status.OK_STATUS;
        if (!target.contains(targetDescriptor)) {
            monitor.subTask("Downloading " + getArtifactKey().getId());
            status = transfer(targetDescriptor, packed != null ? packed : canonical, monitor);

            if (!status.isOK()) {
                target.removeDescriptor(targetDescriptor, monitor);
                if (canonical != packed) {
                    status = transfer(targetDescriptor, canonical, monitor);
                }
            }

            if (!status.isOK()) {
                target.removeDescriptor(targetDescriptor, monitor);
            }
        }

        setResult(status);
        return;
    }

    private boolean contains(IArtifactRepository repository, IArtifactKey artifactKey, boolean packed) {
        for (IArtifactDescriptor descriptor : repository.getArtifactDescriptors(artifactKey)) {
            String format = descriptor.getProperties().get(IArtifactDescriptor.FORMAT);
            if (packed && IArtifactDescriptor.FORMAT_PACKED.equals(format)) {
                return true;
            } else if (!packed && format == null) {
                return true;
            }
        }
        return false;
    }
}
