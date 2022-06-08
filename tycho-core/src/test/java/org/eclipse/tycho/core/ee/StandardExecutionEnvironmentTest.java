/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Before;
import org.junit.Test;

public class StandardExecutionEnvironmentTest {

    private static final int RUNNING_JAVA = Runtime.version().feature();
    private StandardExecutionEnvironment javaSE9Environment;
    private StandardExecutionEnvironment javaSE8Environment;
    private StandardExecutionEnvironment javaSE7Environment;
    private StandardExecutionEnvironment javaSE6Environment;
    private StandardExecutionEnvironment j2SE5Environment;
    private StandardExecutionEnvironment j2SE14Environment;
    private StandardExecutionEnvironment j2SE13Environment;
    private StandardExecutionEnvironment j2SE12Environment;
    private StandardExecutionEnvironment jre11Environment;
    private StandardExecutionEnvironment cdc11Environment;
    private StandardExecutionEnvironment cdc10Environment;
    private StandardExecutionEnvironment osgiMin10Environment;
    private StandardExecutionEnvironment osgiMin11Environment;
    private StandardExecutionEnvironment osgiMin12Environment;
    private StandardExecutionEnvironment javaSECompact1Environment;
    private StandardExecutionEnvironment javaSECompact2Environment;
    private StandardExecutionEnvironment javaSECompact3Environment;

    @Before
    public void setUp() throws Exception {
        javaSECompact1Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE/compact1-1.8", null, null,
                new SilentLog());
        javaSECompact2Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE/compact2-1.8", null, null,
                new SilentLog());
        javaSECompact3Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE/compact3-1.8", null, null,
                new SilentLog());
        javaSE9Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-9", null, null, new SilentLog());
        javaSE8Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.8", null, null,
                new SilentLog());
        javaSE7Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.7", null, null,
                new SilentLog());
        javaSE6Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.6", null, null,
                new SilentLog());
        j2SE5Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.5", null, null, new SilentLog());
        j2SE14Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.4", null, null, new SilentLog());
        j2SE13Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.3", null, null, new SilentLog());
        j2SE12Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.2", null, null, new SilentLog());
        jre11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JRE-1.1", null, null, new SilentLog());
        cdc11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("CDC-1.1/Foundation-1.1", null, null,
                new SilentLog());
        cdc10Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("CDC-1.0/Foundation-1.0", null, null,
                new SilentLog());
        osgiMin10Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.0", null, null,
                new SilentLog());
        osgiMin11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.1", null, null,
                new SilentLog());
        osgiMin12Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.2", null, null,
                new SilentLog());
    }

    @Test
    public void testNotNull() {
        assertNotNull(getStandardExecutionEnvironment(RUNNING_JAVA));
        assertNotNull(javaSE9Environment);
        assertNotNull(javaSE8Environment);
        assertNotNull(javaSE7Environment);
        assertNotNull(javaSE6Environment);
        assertNotNull(j2SE5Environment);
        assertNotNull(j2SE14Environment);
        assertNotNull(j2SE12Environment);
        assertNotNull(jre11Environment);
        assertNotNull(cdc10Environment);
        assertNotNull(cdc11Environment);
        assertNotNull(osgiMin10Environment);
        assertNotNull(osgiMin11Environment);
        assertNotNull(osgiMin12Environment);
        assertNotNull(javaSECompact1Environment);
        assertNotNull(javaSECompact2Environment);
        assertNotNull(javaSECompact3Environment);
    }

    @Test
    public void testGetProfileName() {
        assertEquals("JavaSE-" + RUNNING_JAVA, getStandardExecutionEnvironment(RUNNING_JAVA).getProfileName());
        assertEquals("JavaSE-9", javaSE9Environment.getProfileName());
        assertEquals("JavaSE-1.8", javaSE8Environment.getProfileName());
        assertEquals("JavaSE-1.7", javaSE7Environment.getProfileName());
        assertEquals("JavaSE-1.6", javaSE6Environment.getProfileName());
        assertEquals("J2SE-1.5", j2SE5Environment.getProfileName());
        assertEquals("J2SE-1.4", j2SE14Environment.getProfileName());
        assertEquals("J2SE-1.3", j2SE13Environment.getProfileName());
        assertEquals("J2SE-1.2", j2SE12Environment.getProfileName());
        assertEquals("JRE-1.1", jre11Environment.getProfileName());
        assertEquals("CDC-1.0/Foundation-1.0", cdc10Environment.getProfileName());
        assertEquals("CDC-1.1/Foundation-1.1", cdc11Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.0", osgiMin10Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.1", osgiMin11Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.2", osgiMin12Environment.getProfileName());
        assertEquals("JavaSE/compact1-1.8", javaSECompact1Environment.getProfileName());
        assertEquals("JavaSE/compact2-1.8", javaSECompact2Environment.getProfileName());
        assertEquals("JavaSE/compact3-1.8", javaSECompact3Environment.getProfileName());
    }

    @Test
    public void testCompilerSourceLevel() {
        assertEquals("1.3", osgiMin10Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", osgiMin11Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", osgiMin12Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", cdc10Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", cdc11Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", jre11Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", j2SE12Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", j2SE13Environment.getCompilerSourceLevelDefault());
        assertEquals("1.3", j2SE14Environment.getCompilerSourceLevelDefault());
        assertEquals("1.5", j2SE5Environment.getCompilerSourceLevelDefault());
        assertEquals("1.6", javaSE6Environment.getCompilerSourceLevelDefault());
        assertEquals("1.7", javaSE7Environment.getCompilerSourceLevelDefault());
        assertEquals("1.8", javaSE8Environment.getCompilerSourceLevelDefault());
        assertEquals("1.8", javaSECompact1Environment.getCompilerSourceLevelDefault());
        assertEquals("1.8", javaSECompact2Environment.getCompilerSourceLevelDefault());
        assertEquals("1.8", javaSECompact3Environment.getCompilerSourceLevelDefault());
        assertEquals("9", javaSE9Environment.getCompilerSourceLevelDefault());
        for (int version = 10; version <= RUNNING_JAVA; version++) {
            assertEquals(String.valueOf(version),
                    getStandardExecutionEnvironment(version).getCompilerSourceLevelDefault());
        }
    }

    @Test
    public void testCompilerTargetLevel() {
        assertEquals("1.1", osgiMin10Environment.getCompilerTargetLevelDefault());
        assertEquals("1.2", osgiMin11Environment.getCompilerTargetLevelDefault());
        assertEquals("1.2", osgiMin12Environment.getCompilerTargetLevelDefault());
        assertEquals("1.1", cdc10Environment.getCompilerTargetLevelDefault());
        assertEquals("1.2", cdc11Environment.getCompilerTargetLevelDefault());
        assertEquals("1.1", jre11Environment.getCompilerTargetLevelDefault());
        assertEquals("1.1", j2SE12Environment.getCompilerTargetLevelDefault());
        assertEquals("1.1", j2SE13Environment.getCompilerTargetLevelDefault());
        assertEquals("1.2", j2SE14Environment.getCompilerTargetLevelDefault());
        assertEquals("1.5", j2SE5Environment.getCompilerTargetLevelDefault());
        assertEquals("1.6", javaSE6Environment.getCompilerTargetLevelDefault());
        assertEquals("1.7", javaSE7Environment.getCompilerTargetLevelDefault());
        assertEquals("1.8", javaSE8Environment.getCompilerTargetLevelDefault());
        assertEquals("1.8", javaSECompact1Environment.getCompilerTargetLevelDefault());
        assertEquals("1.8", javaSECompact2Environment.getCompilerTargetLevelDefault());
        assertEquals("1.8", javaSECompact3Environment.getCompilerTargetLevelDefault());
        assertEquals("9", javaSE9Environment.getCompilerTargetLevelDefault());
        for (int version = 10; version <= RUNNING_JAVA; version++) {
            StandardExecutionEnvironment environment = getStandardExecutionEnvironment(version);
            assertEquals(String.valueOf(version), environment.getCompilerTargetLevelDefault());
        }
    }

    @Test
    public void testCompilerTargetCompatibility() throws Exception {
        assertTrue(j2SE14Environment.isCompatibleCompilerTargetLevel("1.1"));
        assertTrue(j2SE14Environment.isCompatibleCompilerTargetLevel("1.2"));
        assertFalse(j2SE14Environment.isCompatibleCompilerTargetLevel("1.3"));

        // version aliases
        assertTrue(j2SE5Environment.isCompatibleCompilerTargetLevel("5"));
        assertTrue(j2SE5Environment.isCompatibleCompilerTargetLevel("5.0"));
        assertTrue(javaSE6Environment.isCompatibleCompilerTargetLevel("6"));
        assertTrue(javaSE6Environment.isCompatibleCompilerTargetLevel("6.0"));
        assertTrue(javaSE7Environment.isCompatibleCompilerTargetLevel("7"));
        assertTrue(javaSE7Environment.isCompatibleCompilerTargetLevel("7.0"));
        assertTrue(javaSE8Environment.isCompatibleCompilerTargetLevel("8"));
        assertTrue(javaSE8Environment.isCompatibleCompilerTargetLevel("8.0"));
        assertTrue(javaSE9Environment.isCompatibleCompilerTargetLevel("9"));
        assertTrue(javaSE9Environment.isCompatibleCompilerTargetLevel("9.0"));
        for (int version = 10; version <= RUNNING_JAVA; version++) {
            assertTrue(getStandardExecutionEnvironment(version)
                    .isCompatibleCompilerTargetLevel(String.valueOf(version) + ".0"));
        }
    }

    @Test(expected = UnknownEnvironmentException.class)
    public void testUnknownEnv() throws Throwable {
        ExecutionEnvironmentUtils.getExecutionEnvironment("foo", null, null, new SilentLog());
    }

    @Test
    public void testCompare() throws Exception {
        List<StandardExecutionEnvironment> expectedList = new ArrayList<>(Arrays.asList(osgiMin10Environment,
                osgiMin11Environment, osgiMin12Environment, cdc10Environment, cdc11Environment, jre11Environment,
                j2SE12Environment, j2SE13Environment, j2SE14Environment, j2SE5Environment, javaSE6Environment,
                javaSE7Environment, javaSECompact1Environment, javaSECompact2Environment, javaSECompact3Environment,
                javaSE8Environment, javaSE9Environment, getStandardExecutionEnvironment(RUNNING_JAVA)));
        List<StandardExecutionEnvironment> actualList = new ArrayList<>(expectedList);
        Collections.shuffle(actualList);
        Collections.sort(actualList);
        assertEquals(expectedList, actualList);
    }

    @Test
    public void testCompanionJar() throws IOException {
        assertTrue(StandardExecutionEnvironment.getSystemPackagesCompanionJar().isFile());
    }

    private StandardExecutionEnvironment getStandardExecutionEnvironment(int version) {
        return ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-" + version, null, null, new SilentLog());
    }
}
