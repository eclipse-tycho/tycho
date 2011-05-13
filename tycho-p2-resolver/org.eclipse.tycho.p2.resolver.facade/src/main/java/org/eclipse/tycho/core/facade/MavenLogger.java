/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - extended interface for more general use
 *******************************************************************************/
package org.eclipse.tycho.core.facade;

/**
 * Maven logger for Tycho OSGi runtime.
 */
public interface MavenLogger {
    public void warn(String message, Throwable cause);

    public void info(String message);

    public void debug(String message);

    public boolean isDebugEnabled();

    public boolean isExtendedDebugEnabled();
}
