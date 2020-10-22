/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.core.resolver.shared;

public enum OptionalResolutionAction {
    /**
     * Treat optional dependencies as required.
     **/
    REQUIRE,

    /**
     * Ignore optional dependencies.
     */
    IGNORE,

    /**
     * Treat optional dependencies as optional.
     */
    OPTIONAL,
}
