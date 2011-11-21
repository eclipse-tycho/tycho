/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.core.UnknownEnvironmentException;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.junit.Before;
import org.junit.Test;

public class ExecutionEnvironmentTest {

    private ExecutionEnvironment javaSE6Enviroment;
    private ExecutionEnvironment javaSE7Enviroment;
    private ExecutionEnvironment j2SE5Enviroment;
    private ExecutionEnvironment j2SE14Environment;
    private ExecutionEnvironment j2SE13Environment;
    private ExecutionEnvironment j2SE12Environment;
    private ExecutionEnvironment jre11Environment;
    private ExecutionEnvironment cdc11Environment;
    private ExecutionEnvironment cdc10Environment;
    private ExecutionEnvironment osgiMin10Environment;
    private ExecutionEnvironment osgiMin11Environment;
    private ExecutionEnvironment osgiMin12Environment;

    @Before
    public void setUp() throws Exception {
        javaSE6Enviroment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.6");
        javaSE7Enviroment = ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.7");
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
        assertEquals("1.3", osgiMin10Environment.getCompilerSourceLevel());
        assertEquals("1.3", osgiMin11Environment.getCompilerSourceLevel());
        assertEquals("1.3", osgiMin12Environment.getCompilerSourceLevel());
        assertEquals("1.3", cdc10Environment.getCompilerSourceLevel());
        assertEquals("1.3", cdc11Environment.getCompilerSourceLevel());
        assertEquals("1.3", jre11Environment.getCompilerSourceLevel());
        assertEquals("1.3", j2SE12Environment.getCompilerSourceLevel());
        assertEquals("1.3", j2SE13Environment.getCompilerSourceLevel());
        assertEquals("1.3", j2SE14Environment.getCompilerSourceLevel());
        assertEquals("1.5", j2SE5Enviroment.getCompilerSourceLevel());
        assertEquals("1.6", javaSE6Enviroment.getCompilerSourceLevel());
    }

    @Test
    public void testCompilerTargetLevel() {
        assertEquals("1.1", osgiMin10Environment.getCompilerTargetLevel());
        assertEquals("1.2", osgiMin11Environment.getCompilerTargetLevel());
        assertEquals("1.2", osgiMin12Environment.getCompilerTargetLevel());
        assertEquals("1.1", cdc10Environment.getCompilerTargetLevel());
        assertEquals("1.2", cdc11Environment.getCompilerTargetLevel());
        assertEquals("1.1", jre11Environment.getCompilerTargetLevel());
        assertEquals("1.1", j2SE12Environment.getCompilerTargetLevel());
        assertEquals("1.1", j2SE13Environment.getCompilerTargetLevel());
        assertEquals("1.2", j2SE14Environment.getCompilerTargetLevel());
        assertEquals("1.5", j2SE5Enviroment.getCompilerTargetLevel());
        assertEquals("1.6", javaSE6Enviroment.getCompilerTargetLevel());
    }

    @Test
    public void testUnknownEnv() {
        try {
            ExecutionEnvironmentUtils.getExecutionEnvironment("foo");
            fail();
        } catch (UnknownEnvironmentException e) {
            // expected
        }
    }

    @Test
    public void testCompare() throws Exception {
        List<ExecutionEnvironment> expectedList = new ArrayList<ExecutionEnvironment>(Arrays.asList(
                osgiMin10Environment, osgiMin11Environment, osgiMin12Environment, cdc10Environment, cdc11Environment,
                jre11Environment, j2SE12Environment, j2SE13Environment, j2SE14Environment, j2SE5Enviroment,
                javaSE6Enviroment, javaSE7Enviroment));
        List<ExecutionEnvironment> actualList = new ArrayList<ExecutionEnvironment>(expectedList);
        Collections.shuffle(actualList);
        Collections.sort(actualList);
        assertEquals(expectedList, actualList);
    }
}
