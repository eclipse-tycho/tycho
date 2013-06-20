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
package org.eclipse.tycho.repository.streaming.testutil;

import java.io.IOException;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

public class ProbeRawArtifactSink extends ProbeArtifactSink implements IRawArtifactSink {

    private IArtifactDescriptor artifactDescriptor;

    public static ProbeRawArtifactSink newRawArtifactSinkFor(IArtifactDescriptor artifactDescriptor) {
        return new ProbeRawArtifactSink(artifactDescriptor);
    }

    public ProbeRawArtifactSink(IArtifactDescriptor artifactDescriptor) {
        super(artifactDescriptor.getArtifactKey());
        this.artifactDescriptor = artifactDescriptor;
    }

    public IArtifactDescriptor getArtifactFormatToBeWritten() {
        return artifactDescriptor;
    }

    public String md5AsHex() throws IOException {
        if (!writeIsCommitted()) {
            return null;
        }
        return sink.md5AsHex();
    }

}
