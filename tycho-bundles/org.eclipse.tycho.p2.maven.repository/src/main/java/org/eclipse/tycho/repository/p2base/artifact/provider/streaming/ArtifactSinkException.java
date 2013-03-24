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

/**
 * Exception thrown when one of the operations of {@link IArtifactSink} fails.
 */
public class ArtifactSinkException extends Exception {
    private static final long serialVersionUID = 1L;

    public ArtifactSinkException(String message) {
        super(message);
    }

    public ArtifactSinkException(String message, Throwable cause) {
        super(message, cause);
    }

}
