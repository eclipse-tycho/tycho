/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.util;

import java.io.File;

import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;

public class BuildPropertiesParserForTesting implements BuildPropertiesParser {

    public BuildProperties parse(File baseDir) {
        return new BuildPropertiesImpl(new File(baseDir, "build.properties"));
    }

}
