/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.jdt.internal.compiler.batch.ClasspathJar;
import org.eclipse.jdt.internal.compiler.batch.ClasspathLocation;
import org.junit.Before;
import org.junit.Test;

public class CompilerMainTest {

    private CompilerMain compiler;

    @Before
    public void setup() {
        compiler = new CompilerMain(null, null, false, new ConsoleLogger(Logger.LEVEL_WARN, "test"));
        compiler.setJavaHome(new File(getClass().getResource("/test-jdk/").getFile()));
    }

    @Test
    public void testHandleEndorseddirs() {
        ArrayList endorsedDirClasspaths = compiler.handleEndorseddirs(null);
        assertSingleClasspathEntry(endorsedDirClasspaths, "/lib/endorsed/endorsed");
    }

    @Test
    public void testHandleExtdirs() {
        ArrayList extDirs = compiler.handleExtdirs(null);
        assertSingleClasspathEntry(extDirs, "/lib/ext/ext");
    }

    @Test
    public void testHandleBootclasspath() {
        ArrayList bootClasspath = compiler.handleBootclasspath(null, null);
        assertEquals(2, bootClasspath.size());
        Set<String> actual = new HashSet<String>();
        boolean foundRtJar = false;
        boolean foundAnotherJar = false;
        for (Object cpo : bootClasspath) {
            assertTrue(cpo instanceof ClasspathJar);
            String path = new String(((ClasspathJar) cpo).normalizedPath());
            if (path.endsWith("/lib/rt")) {
                foundRtJar = true;
            } else if (path.endsWith("/lib/another")) {
                foundAnotherJar = true;
            }
        }
        assertTrue("lib/rt.jar not found in bootClassPath", foundRtJar);
        assertTrue("lib/another.jar not found in bootClassPath", foundAnotherJar);
    }

    private void assertSingleClasspathEntry(ArrayList classpath, String expectedPath) {
        assertEquals(1, classpath.size());
        ClasspathLocation classPathEntry = (ClasspathLocation) classpath.get(0);
        assertTrue(classPathEntry instanceof ClasspathJar);
        String path = new String(((ClasspathJar) classPathEntry).normalizedPath());
        assertTrue(expectedPath + " not found in classpath", path.endsWith(expectedPath));
    }

}
