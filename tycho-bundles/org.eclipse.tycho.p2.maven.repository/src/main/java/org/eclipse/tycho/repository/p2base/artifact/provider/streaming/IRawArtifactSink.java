/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider.streaming;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Specialization of {@link IArtifactSink} for receiving an artifact in a raw, repository internal
 * storage format.
 * 
 * @see IArtifactSink
 * @see ArtifactSinkFactory
 */
public interface IRawArtifactSink extends IArtifactSink {

    /**
     * Returns the format (and key) of the artifact to be written.
     * 
     * <p>
     * Note: The artifact key that can be obtained via this method is the same as the key returned
     * by {@link #getArtifactToBeWritten()}, i.e.
     * <code>sink.getArtifactDescriptorToBeWritten().getArtifactKey().equals(sink.getArtifactToBeWritten())</code>
     * holds true.
     * </p>
     */
    public IArtifactDescriptor getArtifactFormatToBeWritten();

}
