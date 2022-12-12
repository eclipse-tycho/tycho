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
