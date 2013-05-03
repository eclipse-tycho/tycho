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

class StreamArtifactSink implements IArtifactSink {

    private IArtifactKey requestedKey;
    private OutputStream destination;
    private boolean writeStarted = false;

    StreamArtifactSink(IArtifactKey requestedKey, OutputStream destination) {
        this.requestedKey = requestedKey;
        this.destination = destination;
    }

    public IArtifactKey getArtifactToBeWritten() {
        return requestedKey;
    }

    public boolean canBeginWrite() {
        return !writeStarted;
    }

    public OutputStream beginWrite() throws ArtifactSinkException {
        if (writeStarted) {
            throw new IllegalStateException("Cannot re-start write operation");
        }
        writeStarted = true;
        return destination;
    }

    public void commitWrite() throws ArtifactSinkException {
        // do nothing; creator is responsible for closing the output stream
    }

    public void abortWrite() throws ArtifactSinkException {
        // bad data has been written to the output stream -> this can't be undone, so do nothing
    }

}
