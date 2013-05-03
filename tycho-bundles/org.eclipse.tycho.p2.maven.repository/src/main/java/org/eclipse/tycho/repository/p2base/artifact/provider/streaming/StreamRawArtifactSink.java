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

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

final class StreamRawArtifactSink extends StreamArtifactSink implements IRawArtifactSink {

    private IArtifactDescriptor requestedDescriptor;

    StreamRawArtifactSink(IArtifactDescriptor requestedDescriptor, OutputStream destination) {
        super(requestedDescriptor.getArtifactKey(), destination);
        this.requestedDescriptor = requestedDescriptor;
    }

    public IArtifactDescriptor getArtifactFormatToBeWritten() {
        return requestedDescriptor;
    }
}
