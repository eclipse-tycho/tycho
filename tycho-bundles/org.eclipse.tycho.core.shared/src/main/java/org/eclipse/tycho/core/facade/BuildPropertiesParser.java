/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.facade;

import java.io.File;

public interface BuildPropertiesParser {

    /**
     * Parse the file "build.properties" in baseDir. If the file does not exist or cannot be read,
     * an "empty" {@link BuildProperties} will be returned.
     * 
     */
    public BuildProperties parse(File baseDir);

}
