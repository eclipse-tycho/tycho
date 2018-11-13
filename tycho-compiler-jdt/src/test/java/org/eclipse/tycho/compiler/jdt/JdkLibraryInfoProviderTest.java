/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Enumeration;

import org.apache.maven.plugin.testing.SilentLog;
import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;
import org.junit.Test;

public class JdkLibraryInfoProviderTest {

    @Test
    public void testGetLibraryInfoForCurrentlyRunningJDK() throws Exception {
        JdkLibraryInfoProviderStub libInfoProvider = new JdkLibraryInfoProviderStub(
                new File("../tycho-lib-detector/target/classes/"), new SilentLog());
        String javaHome = System.getProperty("java.home");
        LibraryInfo libraryInfo = libInfoProvider.getLibraryInfo(javaHome);

        assertEquals(System.getProperty("java.version"), libraryInfo.getVersion());
        String expectedBootclasspath = getExpectedBootclasspath(javaHome);
        assertEquals(expectedBootclasspath, String.join(File.pathSeparator, libraryInfo.getBootpath()));
        String javaExtDirs = System.getProperty("java.ext.dirs");
        if (javaExtDirs != null) {
            assertEquals(javaExtDirs, String.join(File.pathSeparator, libraryInfo.getExtensionDirs()));
        }
        String javaEndorsedDirs = System.getProperty("java.endorsed.dirs");
        if (javaExtDirs != null) {
            assertEquals(javaEndorsedDirs, String.join(File.pathSeparator, libraryInfo.getEndorsedDirs()));
        }
    }

    @Test
    public void testGetLibraryInfoForFakeJDKWithoutJavaExecutable() throws Exception {
        JdkLibraryInfoProviderStub libInfoProvider = new JdkLibraryInfoProviderStub(
                new File("../tycho-lib-detector/target/classes/"), new SilentLog());
        LibraryInfo libInfo = libInfoProvider
                .getLibraryInfo(new File("src/test/resources/testJavaHome").getAbsolutePath());
        assertEquals("unknown", libInfo.getVersion());
        String[] bootpath = libInfo.getBootpath();
        assertEquals(2, bootpath.length);
        assertTrue(bootpath[0].endsWith("lib" + File.separator + "some.jar"));
        assertTrue(bootpath[1].endsWith("lib" + File.separator + "ext" + File.separator + "another.jar"));
        assertArrayEquals(new String[0], libInfo.getEndorsedDirs());
        assertArrayEquals(new String[0], libInfo.getExtensionDirs());
    }

    private String getExpectedBootclasspath(String javaHome) {
        String propertyKey = null;
        Enumeration keys = System.getProperties().keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.endsWith(".boot.class.path")) { //$NON-NLS-1$
                propertyKey = key;
                break;
            }
        }
        if (propertyKey != null) {
            // Java version <= 8
            return System.getProperty(propertyKey);
        } else {
            // Java version >= 9
            File jrtFsJar = new File(javaHome, "lib/jrt-fs.jar");
            if (jrtFsJar.isFile()) {
                return jrtFsJar.getAbsolutePath();
            } else {
                return new File(javaHome, "jrt-fs.jar").getAbsolutePath();
            }
        }
    }
}
