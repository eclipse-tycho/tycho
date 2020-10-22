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
 *    SAP AG - extended interface for more general use
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

/**
 * Maven logger for Tycho OSGi runtime.
 */
public interface MavenLogger {

    public void error(String message);

    public void warn(String message);

    public void warn(String message, Throwable cause);

    public void info(String message);

    public void debug(String message);

    public boolean isDebugEnabled();

    public boolean isExtendedDebugEnabled();

}
