/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.osgi.framework;

import java.util.Set;

public record Bundles(Set<String> bundles) {

    public static final String BUNDLE_API_TOOLS = "org.eclipse.pde.api.tools";
    public static final String BUNDLE_ECLIPSE_HELP_BASE = "org.eclipse.help.base";
    public static final String BUNDLE_PDE_CORE = "org.eclipse.pde.core";
    static final String BUNDLE_LAUNCHING_MACOS = "org.eclipse.jdt.launching.macosx";
    static final String BUNDLE_APP = "org.eclipse.equinox.app";
    static final String BUNDLE_SCR = "org.apache.felix.scr";
    static final String BUNDLE_CORE = "org.eclipse.core.runtime";
    static final String BUNDLE_LAUNCHER = "org.eclipse.equinox.launcher";

    public static Bundles of(String... bundles) {
        return new Bundles(Set.of(bundles));
    }
}
