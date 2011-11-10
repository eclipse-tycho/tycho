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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.jdt.internal.compiler.batch.ClasspathJar;
import org.junit.Before;
import org.junit.Test;

public class CompilerMainTest {

    private static class TestCompilerMain extends CompilerMain {

        private boolean isMacOS = false;

        public TestCompilerMain(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished,
                org.codehaus.plexus.logging.Logger logger) {
            super(outWriter, errWriter, systemExitWhenFinished, logger);
        }

        public void setMacOS(boolean isMacOS) {
            this.isMacOS = isMacOS;
        }

        @Override
        protected boolean isMacOS() {
            return isMacOS;
        }

    }

    private TestCompilerMain compiler;

    @Before
    public void setup() {
        compiler = new TestCompilerMain(null, null, false, new ConsoleLogger(Logger.LEVEL_WARN, "test"));
        compiler.setJavaHome(new File(getClass().getResource("/test-jdk/").getFile()));
    }

    @Test
    public void testHandleEndorseddirs() {
        ArrayList endorsedDirClasspaths = compiler.handleEndorseddirs(null);
        checkClassPath(endorsedDirClasspaths, "/lib/endorsed/endorsed");
    }

    @Test
    public void testHandleExtdirs() {
        ArrayList extDirs = compiler.handleExtdirs(null);
        checkClassPath(extDirs, "/lib/ext/ext");
    }

    @Test
    public void testHandleBootClasspathNonMacOS() {
        ArrayList bootClasspath = compiler.handleBootclasspath(null, null);
        checkClassPath(bootClasspath, "/lib/rt", "/lib/another");
    }

    @Test
    public void testHandleBootClasspathMacOS() {
        compiler.setMacOS(true);
        ArrayList bootClasspath = compiler.handleBootclasspath(null, null);
        checkClassPath(bootClasspath, "/Classes/classes");
    }

    private void checkClassPath(ArrayList classpath, String... expectedRelativePaths) {
        assertEquals(expectedRelativePaths.length, classpath.size());
        Set<String> actualPaths = new HashSet<String>();
        for (Object cpo : classpath) {
            assertTrue(cpo instanceof ClasspathJar);
            String path = new String(((ClasspathJar) cpo).normalizedPath());
            actualPaths.add(path);
        }
        for (String expectedPath : expectedRelativePaths) {
            for (Iterator<String> iterator = actualPaths.iterator(); iterator.hasNext();) {
                String actualPath = iterator.next();
                if (actualPath.endsWith(expectedPath)) {
                    iterator.remove();
                }
            }
        }
        assertEquals(0, actualPaths.size());
    }

}
