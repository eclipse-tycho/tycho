/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

public class EquinoxLaunchingException extends RuntimeException {
    private static final long serialVersionUID = -2582656444738672521L;

    public EquinoxLaunchingException(Exception cause) {
        super(cause);
    }

    public EquinoxLaunchingException(String message, Exception cause) {
        super(message, cause);
    }
}
