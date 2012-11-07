/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class StandardExecutionEnvironmentTest {

    private StandardExecutionEnvironment javaSE6Enviroment;
    private StandardExecutionEnvironment javaSE7Enviroment;
    private StandardExecutionEnvironment j2SE5Enviroment;
    private StandardExecutionEnvironment j2SE14Environment;
    private StandardExecutionEnvironment j2SE13Environment;
    private StandardExecutionEnvironment j2SE12Environment;
    private StandardExecutionEnvironment jre11Environment;
    private StandardExecutionEnvironment cdc11Environment;
    private StandardExecutionEnvironment cdc10Environment;
    private StandardExecutionEnvironment osgiMin10Environment;
    private StandardExecutionEnvironment osgiMin11Environment;
    private StandardExecutionEnvironment osgiMin12Environment;

    @Before
    public void setUp() throws Exception {
        javaSE7Enviroment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.7");
        javaSE6Enviroment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.6");
        j2SE5Enviroment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.5");
        j2SE14Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.4");
        j2SE13Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.3");
        j2SE12Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.2");
        jre11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("JRE-1.1");
        cdc11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("CDC-1.1/Foundation-1.1");
        cdc10Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("CDC-1.0/Foundation-1.0");
        osgiMin10Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.0");
        osgiMin11Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.1");
        osgiMin12Environment = ExecutionEnvironmentUtils.getExecutionEnvironment("OSGi/Minimum-1.2");
    }

    @Test
    public void testNotNull() {
        assertNotNull(javaSE7Enviroment);
        assertNotNull(javaSE6Enviroment);
        assertNotNull(j2SE5Enviroment);
        assertNotNull(j2SE14Environment);
        assertNotNull(j2SE13Environment);
        assertNotNull(j2SE12Environment);
        assertNotNull(jre11Environment);
        assertNotNull(cdc10Environment);
        assertNotNull(cdc11Environment);
        assertNotNull(osgiMin10Environment);
        assertNotNull(osgiMin11Environment);
        assertNotNull(osgiMin12Environment);
    }

    @Test
    public void testGetProfileName() {
        assertEquals("JavaSE-1.7", javaSE7Enviroment.getProfileName());
        assertEquals("JavaSE-1.6", javaSE6Enviroment.getProfileName());
        assertEquals("J2SE-1.5", j2SE5Enviroment.getProfileName());
        assertEquals("J2SE-1.4", j2SE14Environment.getProfileName());
        assertEquals("J2SE-1.3", j2SE13Environment.getProfileName());
        assertEquals("J2SE-1.2", j2SE12Environment.getProfileName());
        assertEquals("JRE-1.1", jre11Environment.getProfileName());
        assertEquals("CDC-1.0/Foundation-1.0", cdc10Environment.getProfileName());
        assertEquals("CDC-1.1/Foundation-1.1", cdc11Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.0", osgiMin10Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.1", osgiMin11Environment.getProfileName());
        assertEquals("OSGi/Minimum-1.2", osgiMin12Environment.getProfileName());
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
        assertEquals("1.5", j2SE5Enviroment.getCompilerSourceLevelDefault());
        assertEquals("1.6", javaSE6Enviroment.getCompilerSourceLevelDefault());
        assertEquals("1.7", javaSE7Enviroment.getCompilerSourceLevelDefault());
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
        assertEquals("1.5", j2SE5Enviroment.getCompilerTargetLevelDefault());
        assertEquals("1.6", javaSE6Enviroment.getCompilerTargetLevelDefault());
        assertEquals("1.7", javaSE7Enviroment.getCompilerTargetLevelDefault());
    }

    @Test
    public void testCompilerTargetCompatibility() throws Exception {
        assertTrue(j2SE14Environment.isCompatibleCompilerTargetLevel("1.1"));
        assertTrue(j2SE14Environment.isCompatibleCompilerTargetLevel("1.2"));
        assertFalse(j2SE14Environment.isCompatibleCompilerTargetLevel("1.3"));

        // version aliases
        assertTrue(j2SE5Enviroment.isCompatibleCompilerTargetLevel("5"));
        assertTrue(j2SE5Enviroment.isCompatibleCompilerTargetLevel("5.0"));
        assertTrue(javaSE6Enviroment.isCompatibleCompilerTargetLevel("6"));
        assertTrue(javaSE6Enviroment.isCompatibleCompilerTargetLevel("6.0"));
        assertTrue(javaSE7Enviroment.isCompatibleCompilerTargetLevel("7"));
        assertTrue(javaSE7Enviroment.isCompatibleCompilerTargetLevel("7.0"));
    }

    @Test(expected = UnknownEnvironmentException.class)
    public void testUnknownEnv() throws Throwable {
        ExecutionEnvironmentUtils.getExecutionEnvironment("foo");
    }

    @Test
    public void testCompare() throws Exception {
        List<StandardExecutionEnvironment> expectedList = new ArrayList<StandardExecutionEnvironment>(Arrays.asList(
                osgiMin10Environment, osgiMin11Environment, osgiMin12Environment, cdc10Environment, cdc11Environment,
                jre11Environment, j2SE12Environment, j2SE13Environment, j2SE14Environment, j2SE5Enviroment,
                javaSE6Enviroment, javaSE7Enviroment));
        List<StandardExecutionEnvironment> actualList = new ArrayList<StandardExecutionEnvironment>(expectedList);
        Collections.shuffle(actualList);
        Collections.sort(actualList);
        assertEquals(expectedList, actualList);
    }
}
