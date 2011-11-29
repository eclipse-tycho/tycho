/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

/**
 * Wrapper for checked exceptions from the OSGi world.
 */
public class FacadeException extends Exception {
    private static final long serialVersionUID = 1864994424422146579L;

    public FacadeException(Throwable cause) {
        super(cause.getClass().getSimpleName() + " in OSGi bundle code", cause);
    }

    public FacadeException(String message, Throwable exception) {
        super(message, exception);
    }
}
