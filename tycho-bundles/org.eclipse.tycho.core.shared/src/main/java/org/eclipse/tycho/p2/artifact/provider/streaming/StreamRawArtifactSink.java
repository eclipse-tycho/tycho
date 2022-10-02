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

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

final class StreamRawArtifactSink extends StreamArtifactSink implements IRawArtifactSink {

    private IArtifactDescriptor requestedDescriptor;

    StreamRawArtifactSink(IArtifactDescriptor requestedDescriptor, OutputStream destination) {
        super(requestedDescriptor.getArtifactKey(), destination);
        this.requestedDescriptor = requestedDescriptor;
    }

    @Override
    public IArtifactDescriptor getArtifactFormatToBeWritten() {
        return requestedDescriptor;
    }
}
