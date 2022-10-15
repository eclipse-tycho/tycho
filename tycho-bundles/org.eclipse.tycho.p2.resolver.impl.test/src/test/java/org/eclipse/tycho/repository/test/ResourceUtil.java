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
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test;

import java.io.File;
import java.net.URI;

/**
 * Helper class for accessing test resources.
 */
public class ResourceUtil {

    /**
     * p2 repository resources used for multiple tests.
     */
    public enum P2Repositories {
        ECLIPSE_342("e342"), PACK_GZ("packgz");

        private final String path;

        P2Repositories(String path) {
            this.path = path;
        }

        public URI toURI() {
            return resourceFile("repositories/" + path).toURI();

        }

        @Override
        public String toString() {
            return toURI().toString();
        }

    }

    public static File resourceFile(String path) {
        File resolvedFile = new File("resources", path).getAbsoluteFile();

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + path
                    + "\" not found under \"resources\" in the project");
        }
        return resolvedFile;
    }
}
