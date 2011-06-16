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

import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class P2RepositoryCacheImpl implements P2RepositoryCache {

    private HashMap<URI, SoftReference<IArtifactRepository>> artifactRepositories = new HashMap<URI, SoftReference<IArtifactRepository>>();

    private HashMap<URI, SoftReference<IMetadataRepository>> metadataRepositories = new HashMap<URI, SoftReference<IMetadataRepository>>();

    private HashMap<String, TychoRepositoryIndex> indexes = new HashMap<String, TychoRepositoryIndex>();

    public IArtifactRepository getArtifactRepository(URI uri) {
        return dereference(artifactRepositories.get(uri));
    }

    private <T> T dereference(SoftReference<T> reference) {
        return reference != null ? reference.get() : null;
    }

    public IMetadataRepository getMetadataRepository(URI uri) {
        return dereference(metadataRepositories.get(uri));
    }

    public void putRepository(URI uri, IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
        if (metadataRepository != null) {
            metadataRepositories.put(uri, new SoftReference<IMetadataRepository>(metadataRepository));
        }

        if (artifactRepository != null) {
            artifactRepositories.put(uri, new SoftReference<IArtifactRepository>(artifactRepository));
        }
    }

    public TychoRepositoryIndex getRepositoryIndex(String repositoryKey) {
        return indexes.get(repositoryKey);
    }

    public void putRepositoryIndex(String repositoryKey, TychoRepositoryIndex index) {
        indexes.put(repositoryKey, index);
    }
}
