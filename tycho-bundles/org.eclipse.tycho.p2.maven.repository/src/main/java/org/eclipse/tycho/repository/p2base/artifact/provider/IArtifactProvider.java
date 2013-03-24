/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * Provider for artifact content.
 */
public interface IArtifactProvider extends IQueryable<IArtifactKey> {

    /**
     * Returns <code>true</code> if this is a provider for the given artifact.
     * 
     * @param key
     *            an artifact key
     * @return <code>true</code> if this instance can provide the artifact for the given key
     */
    public boolean contains(IArtifactKey key);

    /**
     * Writes the requested artifact to the given {@link IArtifactSink}. The implementation is free
     * to pick the most suitable internal storage format to serve the request, e.g. it may extract
     * the artifact from a pack200-compressed format. In case an error is detected while streaming
     * the artifact (e.g. an MD5 checksum error), the implementation shall re-attempt the read from
     * all other available sources.
     * 
     * @param sink
     *            a sink for a specific artifact
     * @param monitor
     *            a progress monitor, or <code>null</code>
     * @throws ArtifactSinkException
     *             if that exception is thrown by the given {@link IArtifactSink}
     * 
     * @see IArtifactSink#getArtifactToBeWritten()
     */
    // TODO assert status <-> committed relationship
    // TODO implement&document returned warnings
    public IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException;

}
