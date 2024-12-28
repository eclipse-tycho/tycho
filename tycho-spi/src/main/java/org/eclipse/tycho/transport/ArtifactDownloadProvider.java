/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.transport;

import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * An {@link ArtifactDownloadProvider} can supply an alternative caching strategy for artifacts
 * fetched from P2
 */
public interface ArtifactDownloadProvider {

    /**
     * Ask the provider to download the given artifact and transfer it to the given target
     * 
     * @param source
     *            the source URI, might be a mirror URL
     * @param target
     *            the target where the result should be transfered to
     * @param descriptor
     *            the artifact descriptor to be queried
     * @return a status of type cancel if this provider can't supply the artifact, or a
     *         {@link DownloadStatus} in case of success to supply additional information to P2, or
     *         an error if the download failed even though it should not.
     */
    public IStatus downloadArtifact(URI source, OutputStream target, IArtifactDescriptor descriptor);

    /**
     * @return the priority, higher values are considered first
     */
    int getPriority();

}
