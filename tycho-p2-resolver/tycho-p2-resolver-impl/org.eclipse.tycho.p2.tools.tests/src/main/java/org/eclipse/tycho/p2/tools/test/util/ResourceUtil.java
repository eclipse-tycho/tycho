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
package org.eclipse.tycho.p2.tools.test.util;

import java.io.File;
import java.io.IOException;

/**
 * Helper for accessing test resources.
 */
public class ResourceUtil {

    public static File resolveTestResource(String pathRelativeToProjectRoot) throws IllegalStateException {
        File resolvedFile;
        try {
            resolvedFile = new File(pathRelativeToProjectRoot).getCanonicalFile();
        } catch (IOException e) {
            // this should not happen in the expected test setup
            throw new IllegalStateException("I/O error while resolving test resource \"" + pathRelativeToProjectRoot
                    + "\" " + workingDirMessage(), e);
        }

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + pathRelativeToProjectRoot + "\" is not available; "
                    + workingDirMessage());
        }
        return resolvedFile;
    }

    private static String workingDirMessage() {
        return "(working directory is \"" + new File(".").getAbsolutePath() + "\")";
    }
}
