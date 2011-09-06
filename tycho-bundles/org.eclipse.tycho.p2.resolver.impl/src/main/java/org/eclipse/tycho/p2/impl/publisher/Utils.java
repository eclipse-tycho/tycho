/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Utils {

    public static Properties loadBuildProperties(File projectRootDir) {
        File file = new File(projectRootDir, "build.properties");

        Properties buildProperties = new Properties();
        if (file.canRead()) {
            InputStream is = null;
            try {
                try {
                    is = new FileInputStream(file);
                    buildProperties.load(is);
                } finally {
                    if (is != null)
                        is.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return buildProperties;
    }

}
