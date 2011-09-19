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
package org.eclipse.tycho.p2.tools.director;

public interface DirectorApplicationWrapper {
    /**
     * @see org.eclipse.equinox.app.IApplication#EXIT_OK
     */
    public static final Integer EXIT_OK = Integer.valueOf(0);

    /**
     * @see org.eclipse.equinox.internal.p2.director.app.DirectorApplication#run(String[] )
     */
    Object run(String[] args);
}
