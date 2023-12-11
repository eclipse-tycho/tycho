/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

/**
 * Maven logger for Tycho OSGi runtime.
 */
public interface MavenLogger {

    default void error(String message) {
        error(message, null);
    }

    default void warn(String message) {
        warn(message, null);
    }

    default void debug(String message) {
        debug(message, null);
    }

    void error(String message, Throwable cause);

    void warn(String message, Throwable cause);

    void info(String message);

    void debug(String message, Throwable cause);

    boolean isDebugEnabled();

    default boolean isExtendedDebugEnabled() {
        return false;
    }

    <T> T adapt(Class<T> adapt);

}
