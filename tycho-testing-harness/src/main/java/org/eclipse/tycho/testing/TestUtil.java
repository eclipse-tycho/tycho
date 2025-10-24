/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class TestUtil {

    public static File getTestResourceLocation(String name) throws IOException {
        File src = new File(getBasedir(), "src/test/resources/" + name);
        assertTrue(src.exists());
        return src;
    }

    // TODO rename to clarify that this creates a copy
    public static File getBasedir(String name) throws IOException {
        File src = new File(getBasedir(), "src/test/resources/" + name);
        File dst = new File(getBasedir(), "target/" + name);

        if (dst.isDirectory()) {
            FileUtils.deleteDirectory(dst);
        } else if (dst.isFile()) {
            if (!dst.delete()) {
                throw new IOException("Can't delete file " + dst.toString());
            }
        }

        FileUtils.copyDirectoryStructure(src, dst);

        return dst;
    }

    private static final class Lazy {
        static {
            final String path = System.getProperty("basedir");
            BASEDIR = null != path ? path : new File("").getAbsolutePath();
        }

        static final String BASEDIR;
    }

    public static String getBasedir() {
        return Lazy.BASEDIR;
    }

}
