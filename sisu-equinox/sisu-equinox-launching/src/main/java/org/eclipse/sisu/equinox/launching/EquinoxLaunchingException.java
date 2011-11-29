/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

public class EquinoxLaunchingException extends RuntimeException {
    private static final long serialVersionUID = -2582656444738672521L;

    public EquinoxLaunchingException(Exception cause) {
        super(cause);
    }
}
