/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider.streaming;

import java.io.OutputStream;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public final class ArtifactSinkFactory {

    /**
     * Returns an {@link IArtifactSink} instance for writing an artifact to an output stream.
     * 
     * <p>
     * Note that {@link IArtifactSink#beginWrite()} can only be called once on the returned artifact
     * sink, i.e. the returned instance has no support for re-starting the write operation in case
     * the first write attempt fails.
     * </p>
     * 
     * @param artifactKey
     *            The key of the artifact to be written.
     * @param destination
     *            The output stream to write the artifact content to. The ownership of the stream is
     *            not transferred, i.e. neither this method nor the returned <tt>IArtifactSink</tt>
     *            will call {@link OutputStream#close()} on the stream.
     */
    public static IArtifactSink writeToStream(IArtifactKey artifactKey, OutputStream destination) {
        return new StreamArtifactSink(artifactKey, destination);
    }

    /**
     * Returns an {@link IRawArtifactSink} instance for writing an artifact in a raw format to the
     * given output stream.
     * 
     * <p>
     * Note that {@link IRawArtifactSink#beginWrite()} can only be called once on the returned
     * artifact sink, i.e. the returned instance has no support for re-starting the write operation
     * in case the first write attempt fails.
     * </p>
     * 
     * @param artifactDescriptor
     *            An {@link IArtifactDescriptor} specifying artifact and format to be written.
     * @param destination
     *            The output stream to write the raw content to. The ownership of the stream is not
     *            transferred, i.e. neither this method nor the returned <tt>IRawArtifactSink</tt>
     *            will call {@link OutputStream#close()} on the stream.
     */
    public static IRawArtifactSink rawWriteToStream(IArtifactDescriptor artifactDescriptor, OutputStream destination) {
        return new StreamRawArtifactSink(artifactDescriptor, destination);
    }

}
