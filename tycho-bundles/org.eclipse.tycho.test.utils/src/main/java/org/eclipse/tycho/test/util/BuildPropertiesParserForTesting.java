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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesImpl;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;

public class BuildPropertiesParserForTesting implements BuildPropertiesParser {

    public BuildProperties parse(File baseDir) {
        Properties props = new Properties();
        readBuildProperties(baseDir, props);

        return new BuildPropertiesImpl(props);
    }

    private void readBuildProperties(File baseDir, Properties props) {
        InputStream is = null;
        try {
            try {
                is = new FileInputStream(new File(baseDir, "build.properties"));
                props.load(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

}
