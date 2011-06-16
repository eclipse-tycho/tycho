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
package org.eclipse.tycho.p2.impl.resolver;

import java.net.URI;

import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * @TODO does p2 cache IMetadata/ArtifactRepository instances already?
 */
public interface P2RepositoryCache {

    public String SERVICE_NAME = null;

    public IMetadataRepository getMetadataRepository(URI location);

    public IArtifactRepository getArtifactRepository(URI location);

    public void putRepository(URI location, IMetadataRepository metadataRepository,
            IArtifactRepository artifactRepository);

}
