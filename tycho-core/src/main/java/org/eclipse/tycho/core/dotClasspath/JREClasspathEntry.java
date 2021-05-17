/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.dotClasspath;

import java.util.Collection;

public interface JREClasspathEntry extends ProjectClasspathEntry {

    static final String JRE_CONTAINER_PATH_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER/";

    boolean isModule();

    Collection<String> getLimitModules();
}
