/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import static org.eclipse.tycho.test.util.TychoMatchers.exists;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class TestUtil {

    public static File getTestResourceLocation(String name) throws IOException {
        File src = new File(PlexusTestCase.getBasedir(), "src/test/resources/" + name);
        assertThat(src, exists());
        return src;
    }

    // TODO rename to clarify that this creates a copy
    public static File getBasedir(String name) throws IOException {
        File src = new File(PlexusTestCase.getBasedir(), "src/test/resources/" + name);
        File dst = new File(PlexusTestCase.getBasedir(), "target/" + name);

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
}
