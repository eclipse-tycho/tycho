/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
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
 * Exception thrown when a dependency of a project cannot be resolved.
 */
public class DependencyResolutionException extends BuildFailureException {

    private static final long serialVersionUID = 1L;

    public DependencyResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DependencyResolutionException(String message) {
        super(message);
    }

}
