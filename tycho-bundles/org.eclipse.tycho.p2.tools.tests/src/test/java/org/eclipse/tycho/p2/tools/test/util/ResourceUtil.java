/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.test.util;

import java.io.File;

/**
 * Canonical test resource access.
 */
public class ResourceUtil {

    public static File resourceFile(String path) throws IllegalStateException {
        File resolvedFile = new File("resources", path).getAbsoluteFile();

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + path
                    + "\" not found under \"resources\" in the project " + workingDirMessage());
        }
        return resolvedFile;
    }

    private static String workingDirMessage() {
        return "(working directory is \"" + new File(".").getAbsolutePath() + "\")";
    }

}
