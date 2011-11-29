/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.embedder.internal;

/**
 * An <code>EquinoxEmbedderException</code> is thrown if something in the otherwise transparent
 * process of running an Equinox instance from within Maven goes wrong.
 */
public class EquinoxEmbedderException extends RuntimeException {

    public EquinoxEmbedderException(String message) {
        super(message);
    }

    public EquinoxEmbedderException(String message, Throwable cause) {
        super(message, cause);
    }

}
