/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * Defines how extra classpath dependencies (from <code>jars.extra.classpath</code> in
 * build.properties) are handled during target platform resolution.
 */
public enum ClasspathDependenciesAction {

    /**
     * Treat extra classpath dependencies as required.
     */
    REQUIRE,

    /**
     * Treat extra classpath dependencies as optional (included if available, not an error if
     * missing).
     */
    OPTIONAL,

    /**
     * Ignore extra classpath dependencies during resolution.
     */
    IGNORE;
}
