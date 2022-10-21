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
package org.eclipse.tycho.test.util;

import java.io.IOException;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.artifact.provider.streaming.IRawArtifactSink;

public class ProbeRawArtifactSink extends ProbeArtifactSink implements IRawArtifactSink {

    private IArtifactDescriptor artifactDescriptor;

    public static ProbeRawArtifactSink newRawArtifactSinkFor(IArtifactDescriptor artifactDescriptor) {
        return new ProbeRawArtifactSink(artifactDescriptor);
    }

    public ProbeRawArtifactSink(IArtifactDescriptor artifactDescriptor) {
        super(artifactDescriptor.getArtifactKey());
        this.artifactDescriptor = artifactDescriptor;
    }

    @Override
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
