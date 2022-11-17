/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

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
