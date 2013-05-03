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

/**
 * Interface for receiving the content of an artifact. In order to support streaming from a remote
 * server where integrity can only be checked after transferring the entire content, there are
 * methods to commit, abort, or retry the write operation.
 * 
 * @see ArtifactSinkFactory
 */
public interface IArtifactSink {

    /**
     * Returns the key of the artifact expected by this instance.
     */
    IArtifactKey getArtifactToBeWritten();

    /**
     * Check if {@link #beginWrite()} can be called on this instance. This method will typically
     * return <code>false</code> if {@link #commitWrite()} has already been called on this instance,
     * or if {@link #beginWrite()} has been called before and the instance doesn't support
     * re-starting the write operation.
     * 
     * @return <code>true</code> if {@link #beginWrite()} can be called on this instance.
     */
    boolean canBeginWrite();

    /**
     * Method for starting the write operation. If this method has been called before, any content
     * written so far will be discarded.
     * 
     * @return the {@link OutputStream} to write the artifact content to. The ownership of the
     *         stream is <b>not</b> transferred to the caller, i.e. {@link OutputStream#close()}
     *         must not be called on the returned instance. Instead, call {@link #commitWrite()} or
     *         {@link #abortWrite()} to free any allocated resources.
     * @throws IllegalStateException
     *             if this instance is not in the right state to start a write operation. This
     *             exception will be thrown if and only if {@link #canBeginWrite()} returns
     *             <code>false</code>.
     * @throws ArtifactSinkException
     *             if an error occurs while starting the write operation.
     */
    OutputStream beginWrite() throws IllegalStateException, ArtifactSinkException;

    /**
     * Method to committing the write operation. Will be called after the entire artifact content
     * has been successfully streamed to the {@link OutputStream} returned by {@link #beginWrite()}.
     * 
     * @throws IllegalStateException
     *             if there is no running write operation, i.e. when this method has already been
     *             called, {@link #abortWrite()} has been called, or {@link #beginWrite()} has not
     *             been called.
     * @throws ArtifactSinkException
     *             if an error occurs while committing the write operation.
     */
    void commitWrite() throws IllegalStateException, ArtifactSinkException;

    /**
     * Method for aborting the write operation. Should be called if an error is detected while
     * streaming the artifact content.
     * 
     * @throws ArtifactSinkException
     *             if an error occurs while aborting the write operation.
     */
    void abortWrite() throws ArtifactSinkException;

}
