/*******************************************************************************
 * Copyright (c) 2014, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - add public constructors
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tycho.repository.util.StatusTool;

/**
 * Exception thrown by Tycho's p2 resolution implementations. It is a checked exception to encourage
 * that the error is logged with context information about the action requiring the resolution.
 */
public class ResolverException extends Exception {

    private static final long serialVersionUID = 1L;
    private final String details;
    private final String selectionContext;

    public ResolverException(String msg, Throwable cause) {
        super(msg, cause);
        selectionContext = "N/A";
        details = "N/A";
    }

    public ResolverException(String details, String selectionContext, Throwable cause) {
        super("See log for details", cause);
        this.details = details;
        this.selectionContext = selectionContext;
    }

    public ResolverException(IStatus status) {
        super("See log for details", StatusTool.findException(status));
        this.details = StatusTool.toLogMessage(status);
        this.selectionContext = "N/A";
    }

    public String getDetails() {
        return details;
    }

    public String getSelectionContext() {
        return selectionContext;
    }

}
