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
package org.eclipse.tycho.core.ee.shared;

public class BuildFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BuildFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildFailureException(String message) {
        super(message);
    }

}
