/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
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

/**
 * Helper for accessing test resources.
 */
public class ResourceUtil {

    public static File resolveTestResource(String pathRelativeToProjectRoot) throws IllegalStateException {
        File resolvedFile = new File(pathRelativeToProjectRoot).getAbsoluteFile();

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
