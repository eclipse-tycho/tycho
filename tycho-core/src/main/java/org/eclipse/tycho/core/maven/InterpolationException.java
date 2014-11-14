/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bachmann electronic - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

public class InterpolationException extends Exception {

    private static final long serialVersionUID = 1L;

    public InterpolationException(Throwable e) {
        super(e);
    }

}
