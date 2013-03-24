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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.junit.Assert.fail;

import java.io.OutputStream;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

@SuppressWarnings("restriction")
public class NonStartableArtifactSink implements IRawArtifactSink {

    public IArtifactKey getArtifactToBeWritten() {
        // dummy key
        return new ArtifactKey("", "", Version.emptyVersion);
    }

    public IArtifactDescriptor getArtifactFormatToBeWritten() {
        return new ArtifactDescriptor(getArtifactToBeWritten());
    }

    public boolean canBeginWrite() {
        return false;
    }

    public OutputStream beginWrite() throws IllegalStateException, ArtifactSinkException {
        fail("Did not expect call to beginWrite()");
        return null;
    }

    public void commitWrite() throws IllegalStateException, ArtifactSinkException {
        fail("Did not expect call to commitWrite()");
    }

    public void abortWrite() throws ArtifactSinkException {
        fail("Did not expect call to abortWrite()");

    }

}
