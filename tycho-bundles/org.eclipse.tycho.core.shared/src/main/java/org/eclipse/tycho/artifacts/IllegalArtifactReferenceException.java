/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts;

import org.eclipse.tycho.ArtifactType;

/**
 * Exception thrown for syntactically incorrect references to Eclipse artifacts, e.g. references
 * with an illegal version string or an unknown type.
 * 
 * @see ArtifactType
 */
public class IllegalArtifactReferenceException extends Exception {

    private static final long serialVersionUID = 1L;

    public IllegalArtifactReferenceException(String message) {
        super(message);
    }
}
