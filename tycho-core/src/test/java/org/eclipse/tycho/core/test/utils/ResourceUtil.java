/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test.utils;

import java.io.File;
import java.io.IOException;

public class ResourceUtil {

    public static File resourceFile(String path) throws IOException {
        File resolvedFile;
        try {
            resolvedFile = new File("src/test/resources", path).getCanonicalFile();
        } catch (IOException e) {
            // this should not happen in the expected test setup
            throw new IllegalStateException("I/O error while resolving test resource \"" + path + "\" ", e);
        }

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + path
                    + "\" not found under \"src/test/resources\" in the project");
        }
        return resolvedFile;
    }
}
