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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

@SuppressWarnings("restriction")
public class MavenMirrorRequest extends MirrorRequest {

    private final LocalArtifactRepository localRepository;

    public MavenMirrorRequest(IArtifactKey key, LocalArtifactRepository localRepository, Transport transport) {
        super(key, localRepository, null, null, transport);

        this.localRepository = localRepository;
    }

    @Override
    public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
        setSourceRepository(sourceRepository);

        // check local repo to avoid duplicate downloads

        if (localRepository.contains(getArtifactKey())) {
            setResult(Status.OK_STATUS);

            return;
        }

        // not a maven repo and not in maven local repo, delegate to p2 implementation

        super.perform(sourceRepository, monitor);
    }

}
