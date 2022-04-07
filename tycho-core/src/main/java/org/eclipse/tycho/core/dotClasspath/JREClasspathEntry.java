/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.dotClasspath;

import java.util.Collection;

public interface JREClasspathEntry extends ProjectClasspathEntry {

    static final String JRE_CONTAINER_PATH = "org.eclipse.jdt.launching.JRE_CONTAINER";

    static final String JRE_CONTAINER_PATH_STANDARDVMTYPE_PREFIX = JRE_CONTAINER_PATH
            + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";

    /**
     * @return <code>true</code> if this is a modular JRE
     */
    boolean isModule();

    /**
     * @return a collection of limited modules or an empty one if no limits are applied
     */
    Collection<String> getLimitModules();

    /**
     * 
     * @return the id of the JRE that is referenced by his entry or <code>null</code> if 'Workspace
     *         default JRE' is to be assumed
     */
    String getJREName();
}
