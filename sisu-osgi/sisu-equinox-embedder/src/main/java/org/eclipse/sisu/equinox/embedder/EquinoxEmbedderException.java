/*******************************************************************************
 * Copyright (c) 2011 SAP SE and others.
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
package org.eclipse.sisu.equinox.embedder;

/**
 * An <code>EquinoxEmbedderException</code> is thrown if something in the otherwise transparent
 * process of running an Equinox instance from within Maven goes wrong.
 */
public class EquinoxEmbedderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EquinoxEmbedderException(String message) {
        super(message);
    }

    public EquinoxEmbedderException(String message, Throwable cause) {
        super(message, cause);
    }

}
