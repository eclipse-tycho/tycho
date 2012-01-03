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
package org.eclipse.tycho.repository.module;

import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;

class PublishingRepositoryViewForWriting extends PublishingRepositoryView {

    private ModuleArtifactRepositoryDelegate writableArtifactRepository;

    PublishingRepositoryViewForWriting(ModuleMetadataRepository metadataRepository,
            ModuleArtifactRepository artifactRepository, WriteSessionContext writeSession) {
        super(metadataRepository, artifactRepository);
        this.writableArtifactRepository = new ModuleArtifactRepositoryDelegate(artifactRepository, writeSession);
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return writableArtifactRepository;
    }
}
