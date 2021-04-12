/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import java.io.File;
import java.io.IOException;

public class ResourceUtil {

    public static File resourceFile(String path) throws IOException {
        File resolvedFile = new File("resources", path).getAbsoluteFile();

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + path
                    + "\" not found under \"resources\" in the project");
        }
        return resolvedFile;
    }
}
