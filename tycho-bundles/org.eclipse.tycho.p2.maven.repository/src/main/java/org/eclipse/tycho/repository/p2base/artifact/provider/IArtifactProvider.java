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

import java.io.OutputStream;

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
     * Writes the artifact to the given output stream.
     * 
     * @param key
     *            the key of the artifact to transfer
     * @param destination
     *            the stream to write the artifact to
     * @param monitor
     *            a progress monitor, or <code>null</code>
     * @return the result of the artifact transfer
     */
    public IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor);

}
