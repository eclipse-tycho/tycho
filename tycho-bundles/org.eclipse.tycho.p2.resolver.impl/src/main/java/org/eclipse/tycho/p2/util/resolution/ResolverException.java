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
package org.eclipse.tycho.p2.util.resolution;

public class ResolverException extends Exception {

    private static final long serialVersionUID = 1L;
    private final String details;
    private final String selectionContext;

    public ResolverException(String details, String selectionContext, Throwable cause) {
        super("See log for details", cause);
        this.details = details;
        this.selectionContext = selectionContext;
    }

    public String getDetails() {
        return details;
    }

    public String getSelectionContext() {
        return selectionContext;
    }

}
