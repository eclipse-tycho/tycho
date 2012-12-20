/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import java.io.File;
import java.io.IOException;
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
        File resolvedFile;
        try {
            resolvedFile = new File("resources", path).getCanonicalFile();
        } catch (IOException e) {
            // this should not happen in the expected test setup
            throw new IllegalStateException("I/O error while resolving test resource \"" + path + "\" ", e);
        }

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException("Test resource \"" + path
                    + "\" not found under \"resources\" in the project");
        }
        return resolvedFile;
    }
}
