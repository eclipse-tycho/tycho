/*******************************************************************************
 * Copyright (c) 2014, 2015 SAP SE and others.
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
package org.eclipse.tycho.core.shared;

/**
 * Exception indicating a user error e.g. an invalid or inconsistent build configuration.
 */
public class BuildFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BuildFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildFailureException(String message) {
        super(message);
    }

}
