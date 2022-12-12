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
package org.eclipse.tycho.p2.artifact.provider.streaming;

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

    @Override
    public IArtifactKey getArtifactToBeWritten() {
        return requestedKey;
    }

    @Override
    public boolean canBeginWrite() {
        return !writeStarted;
    }

    @Override
    public OutputStream beginWrite() throws ArtifactSinkException {
        if (writeStarted) {
            throw new IllegalStateException("Cannot re-start write operation");
        }
        writeStarted = true;
        return destination;
    }

    @Override
    public void commitWrite() throws ArtifactSinkException {
        // do nothing; creator is responsible for closing the output stream
    }

    @Override
    public void abortWrite() throws ArtifactSinkException {
        // bad data has been written to the output stream -> this can't be undone, so do nothing
    }

}
