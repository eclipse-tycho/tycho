/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.core.shared.BuildPropertiesImpl;

public class BuildPropertiesParserForTesting implements BuildPropertiesParser {

    @Override
    public BuildProperties parse(File baseDir, Interpolator interpolator) {
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
