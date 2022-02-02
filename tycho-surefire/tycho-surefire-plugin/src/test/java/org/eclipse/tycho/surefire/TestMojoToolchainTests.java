/*******************************************************************************
 * Copyright (c) 2014, 2015 Bachmann electronics GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.maven.ToolchainProvider.JDKUsage;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMojoToolchainTests {

    private ToolchainManager toolchainManager;
    private AbstractTestMojo testMojo;
    private MavenSession session;
    private MavenProject project;
    private DefaultJavaToolChain breeToolchain;
    private Toolchain systemToolchain;
    private ToolchainProvider toolchainProvider;

    @Before
    public void setUp() throws Exception {
        toolchainManager = mock(ToolchainManager.class);
        session = mock(MavenSession.class);
        breeToolchain = mock(DefaultJavaToolChain.class);
        systemToolchain = mock(Toolchain.class);
        toolchainProvider = mock(ToolchainProvider.class);
        project = new MavenProject();
        testMojo = new TestPluginMojo();
        setParameter(testMojo, "useJDK", JDKUsage.SYSTEM);
        setParameter(testMojo, "toolchainManager", toolchainManager);
        setParameter(testMojo, "toolchainProvider", toolchainProvider);
        setParameter(testMojo, "project", project);
        setParameter(testMojo, "session", session);
    }

    @Test
    public void testGetToolchainWithUseJDKSetToSystemNoToolchainManager() throws Exception {
        setParameter(testMojo, "toolchainManager", null);
        Assert.assertNull(testMojo.getToolchain());
    }

    @Test
    public void testGetToolchainWithUseJDKSetToSystemWithToolchainManager() throws Exception {
        when(toolchainManager.getToolchainFromBuildContext("jdk", session)).thenReturn(systemToolchain);
        Toolchain tc = testMojo.getToolchain();
        Assert.assertEquals(systemToolchain, tc);
    }

    @Test
    public void testGetToolchainWithUseJDKSetToBREE() throws Exception {
        setupWithBree();
        when(toolchainProvider.findMatchingJavaToolChain(session, "myId")).thenReturn(breeToolchain);
        Toolchain tc = testMojo.getToolchain();
        Assert.assertEquals(breeToolchain, tc);
    }

    @Test
    public void testGetToolchainWithUseJDKSetToBREEToolchainNotFound() throws Exception {
        setupWithBree();
        when(toolchainProvider.findMatchingJavaToolChain(session, "myId")).thenReturn(null);
        try {
            testMojo.getToolchain();
            fail("MojoExcecutionException expected since Toolchain could not be found!");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage()
                    .startsWith("useJDK = BREE configured, but no toolchain of type 'jdk' with id 'myId' found"));
        }
    }

    public void setupWithBree() throws Exception {
        setParameter(testMojo, "useJDK", JDKUsage.BREE);
        ExecutionEnvironmentConfiguration envConf = mock(ExecutionEnvironmentConfiguration.class);
        when(envConf.getProfileName()).thenReturn("myId");
        DefaultReactorProject.adapt(project).setContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION,
                envConf);
    }

    private void setParameter(Object object, String variable, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        field.setAccessible(true);
        field.set(object, value);
    }

}
