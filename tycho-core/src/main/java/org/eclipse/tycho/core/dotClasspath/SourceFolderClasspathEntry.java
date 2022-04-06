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

import java.io.File;

public interface SourceFolderClasspathEntry extends ProjectClasspathEntry {

    /**
     * 
     * @return the source folder
     */
    File getSourcePath();

    /**
     * 
     * @return the configured output folder
     */
    File getOutputFolder();
}
