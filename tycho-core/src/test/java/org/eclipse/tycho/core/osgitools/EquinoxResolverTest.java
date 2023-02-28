/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.eclipse.tycho.version.TychoVersion;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

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
        Properties properties = subject.getPlatformProperties(new Properties(), null, null, DUMMY_EE);
        ModuleContainer container = subject.newState(new DefaultDependencyArtifacts(), properties, null, null,
                new EquinoxResolverConfiguration());
        assertEquals(1, container.getModules().stream().map(Module::getCurrentRevision)
                .map(BundleRevision::getSymbolicName).filter(Constants.SYSTEM_BUNDLE_SYMBOLICNAME::equals).count());
    }

    public void testBREEJavaSE11() throws Exception {
        MavenProject javaSE10Project = getProject("projects/javase-11");
        assertEquals("executionenvironment.javase11", javaSE10Project.getArtifactId());
        ReactorProject reactorProject = DefaultReactorProject.adapt(javaSE10Project);
        ExecutionEnvironment ee = TychoProjectUtils.getExecutionEnvironmentConfiguration(reactorProject)
                .getFullSpecification();
        assertEquals("JavaSE-" + Runtime.version().feature(), ee.getProfileName());
        Properties platformProperties = subject.getPlatformProperties(reactorProject, null,
                new DefaultDependencyArtifacts(), ee);
        String executionEnvironments = platformProperties.getProperty("org.osgi.framework.executionenvironment");
        assertTrue(executionEnvironments.contains("JavaSE-10"));
        String capabilities = platformProperties.getProperty("org.osgi.framework.system.capabilities");
        assertTrue(capabilities.contains(
                "osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0"));
    }

    public void testBuildFrameworkBundle() throws Exception {
        MavenProject javaSE10Project = getProject("projects/frameworkBundle/org.eclipse.osgi");
        assertEquals("org.eclipse.osgi", javaSE10Project.getArtifactId());
//        ReactorProject reactorProject = DefaultReactorProject.adapt(javaSE10Project);
//        ExecutionEnvironment ee = TychoProjectUtils.getExecutionEnvironmentConfiguration(reactorProject)
//                .getFullSpecification();
//        assertEquals("JavaSE-11", ee.getProfileName());
//        Properties platformProperties = subject.getPlatformProperties(reactorProject, null,
//                new DefaultDependencyArtifacts(), ee);
//        String executionEnvironments = platformProperties.getProperty("org.osgi.framework.executionenvironment");
//        assertTrue(executionEnvironments.contains("JavaSE-10"));
//        String capabilities = platformProperties.getProperty("org.osgi.framework.system.capabilities");
//        assertTrue(capabilities.contains(
//                "osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0\""));
    }

    public void testBundleNativeCode() throws IOException, Exception {
        MavenProject project = getProject("projects/bundleNativeCode/bundleWithNativeCode");
        assertEquals("test.bundleNativeCode", project.getArtifactId());
    }

    public void testBundleNativeCode_usingAliases() throws IOException, Exception {
        // This project uses the alias "Win10" for the "Windows10" osname, and "amd64" as alias for the "x86-64" processor. 
        // The processor-alias "x86_64" is probably more common but the difference between hyphen and underscore is hard to spot.
        MavenProject project = getProject("projects/bundleNativeCode/bundleWithNativeCodeUsingAliases");
        assertEquals("test.bundleNativeCode.using.aliases", project.getArtifactId());
    }

    //currently do not work anymore!
    public void testBundleNativeCode_usingInvalidAliases() throws IOException, Exception {
        // Negative test to check that a project with invalid aliases fails to resolve
//        try {
//            getProject("projects/bundleNativeCode/bundleWithNativeCodeUsingInvalidAliases");
//            fail("Project must not resolve");
//        } catch (CompoundRuntimeException e) {
//            assertThat(e.getMessage(), containsString(
//                    "Unresolved requirement: Require-Capability: osgi.native; native.paths:List<String>=\"/lib/dummyLib.dll\"; filter:=\"(&(osgi.native.osname~=theBestOS)(osgi.native.processor~=x43))\""));
//        }
    }

    // --- uility methods ---

    private MavenProject getProject(String path) throws IOException, Exception {
        File basedir = getBasedir(path);

        Properties properties = new Properties();
        properties.put("tycho-version", TychoVersion.getTychoVersion());

        List<MavenProject> projects = getSortedProjects(basedir, properties);
        assertEquals(1, projects.size());

        return projects.get(0);
    }
}
