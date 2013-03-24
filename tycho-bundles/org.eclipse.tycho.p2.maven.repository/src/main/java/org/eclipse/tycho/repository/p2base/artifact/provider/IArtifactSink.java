/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import java.io.OutputStream;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

// TODO javadoc
// move to package .io?
public interface IArtifactSink {

    IArtifactKey getArtifactToBeWritten();

    // TODO check == true for IArtifactSinks API params
    boolean canBeginWrite();

    OutputStream beginWrite() throws IllegalStateException, ArtifactSinkException;

    void commitWrite() throws IllegalStateException, ArtifactSinkException;

    void abortWrite() throws ArtifactSinkException;

}
