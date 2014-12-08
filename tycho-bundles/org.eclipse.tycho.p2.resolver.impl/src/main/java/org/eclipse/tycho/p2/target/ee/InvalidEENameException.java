/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.target.ee;

/**
 * Thrown if a custom execution environment name does not comply to format
 * <tt>&lt;name&gt;-&lt;version&gt;</tt>, with name not containing dashes (-) and version being a
 * valid OSGi version.
 */
public class InvalidEENameException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidEENameException(String eeName) {
        super("Not a valid execution environment: " + eeName + ". Expected format: <name>-<version>");
    }
}
