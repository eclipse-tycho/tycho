/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.osgi.framework.BundleException;

public class EquinoxResolverTest extends AbstractTychoMojoTestCase {
    private static final ExecutionEnvironment DUMMY_EE = ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.5",
            null, null, new SilentLog());

    private EquinoxResolver subject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        subject = lookup(EquinoxResolver.class);
    }

    @Override
    protected void tearDown() throws Exception {
        subject = null;

        super.tearDown();
    }

    public void test_noSystemBundle() throws BundleException {
        Properties properties = subject.getPlatformProperties(new Properties(), null, DUMMY_EE);
        State state = subject.newState(new DefaultDependencyArtifacts(), properties, false, null);

        BundleDescription[] bundles = state.getBundles("system.bundle");

        assertEquals(1, bundles.length);
    }

    public void testBREEJavaSE11() throws Exception {
        File basedir = getBasedir("projects/javase-11");
        Properties properties = new Properties();
        properties.put("tycho-version", TychoVersion.getTychoVersion());
        List<MavenProject> projects = getSortedProjects(basedir, properties, null);
        assertEquals(1, projects.size());
        MavenProject javaSE10Project = projects.get(0);
        assertEquals("executionenvironment.javase11", javaSE10Project.getArtifactId());
        ReactorProject reactorProject = DefaultReactorProject.adapt(javaSE10Project);
        ExecutionEnvironment ee = TychoProjectUtils.getExecutionEnvironmentConfiguration(reactorProject)
                .getFullSpecification();
        assertEquals("JavaSE-11", ee.getProfileName());
        Properties platformProperties = subject.getPlatformProperties(reactorProject, new DefaultDependencyArtifacts(),
                ee);
        String executionEnvironments = platformProperties.getProperty("org.osgi.framework.executionenvironment");
        assertTrue(executionEnvironments.contains("JavaSE-10"));
        String capabilities = platformProperties.getProperty("org.osgi.framework.system.capabilities");
        assertTrue(capabilities.contains(
                "osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0\""));
    }

}
