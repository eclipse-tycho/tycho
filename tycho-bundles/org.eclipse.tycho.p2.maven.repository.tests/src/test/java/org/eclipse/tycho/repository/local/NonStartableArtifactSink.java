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
package org.eclipse.tycho.repository.local;

import static org.junit.Assert.fail;

import java.io.OutputStream;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.p2.artifact.provider.streaming.IRawArtifactSink;

@SuppressWarnings("restriction")
public class NonStartableArtifactSink implements IRawArtifactSink {

    @Override
    public IArtifactKey getArtifactToBeWritten() {
        // dummy key
        return new ArtifactKey("", "", Version.emptyVersion);
    }

    @Override
    public IArtifactDescriptor getArtifactFormatToBeWritten() {
        return new ArtifactDescriptor(getArtifactToBeWritten());
    }

    @Override
    public boolean canBeginWrite() {
        return false;
    }

    @Override
    public OutputStream beginWrite() throws IllegalStateException, ArtifactSinkException {
        fail("Did not expect call to beginWrite()");
        return null;
    }

    @Override
    public void commitWrite() throws IllegalStateException, ArtifactSinkException {
        fail("Did not expect call to commitWrite()");
    }

    @Override
    public void abortWrite() throws ArtifactSinkException {
        fail("Did not expect call to abortWrite()");

    }

}
