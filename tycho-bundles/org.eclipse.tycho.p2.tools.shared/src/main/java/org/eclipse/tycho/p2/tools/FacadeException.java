/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

/**
 * Wrapper for checked exceptions from the OSGi world.
 */
// TODO should be a runtime exception, a generic checked exception doesn't make any sense
public class FacadeException extends Exception {
    private static final long serialVersionUID = 1864994424422146579L;

    public FacadeException(Throwable cause) {
        super(cause.getClass().getSimpleName() + " in OSGi bundle code", cause);
    }

    public FacadeException(String message, Throwable exception) {
        super(message, exception);
    }

    public FacadeException(String message) {
        super(message);
    }
}
