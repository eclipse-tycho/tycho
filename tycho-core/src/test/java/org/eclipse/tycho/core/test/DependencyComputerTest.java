/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.ee.CustomExecutionEnvironment;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.DependencyComputer;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.core.utils.MavenSessionUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class DependencyComputerTest extends AbstractTychoMojoTestCase {
    private DependencyComputer dependencyComputer;
    private EquinoxResolver resolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dependencyComputer = lookup(DependencyComputer.class);
        resolver = lookup(EquinoxResolver.class);
    }

    @Override
    protected void tearDown() throws Exception {
        dependencyComputer = null;
        super.tearDown();
    }

    @Test
    public void testExportPackage() throws Exception {
        File basedir = getBasedir("projects/exportpackage");

        Map<File, MavenProject> basedirMap = MavenSessionUtils.getBasedirMap(getSortedProjects(basedir));

        MavenProject project = basedirMap.get(new File(basedir, "bundle"));
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        DependencyArtifacts platform = (DependencyArtifacts) reactorProject
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);

        ExecutionEnvironment executionEnvironment = TychoProjectUtils
                .getExecutionEnvironmentConfiguration(reactorProject).getFullSpecification();
        ModuleContainer state = resolver.newResolvedState(reactorProject, null, executionEnvironment, platform);
        ModuleRevision bundle = state.getModule(project.getBasedir().getAbsolutePath()).getCurrentRevision();

        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(bundle);
        Assert.assertEquals(3, dependencies.size());
        Assert.assertEquals("dep", dependencies.get(0).module.getSymbolicName());
        Assert.assertEquals("dep2", dependencies.get(1).module.getSymbolicName());
        Assert.assertEquals("dep3", dependencies.get(2).module.getSymbolicName());
        Assert.assertTrue(dependencies.get(2).rules.isEmpty());
    }

    @Test
    public void testTYCHO0378unwantedSelfDependency() throws Exception {
        File basedir = getBasedir("projects/TYCHO0378unwantedSelfDependency");
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);

        Assert.assertEquals(0, result.getProject().getDependencies().size());
    }

    // TODO code reuse
    @Test
    public void testWiringToPackageFromCustomProfile() throws Exception {
        File basedir = getBasedir("projects/customProfile");

        Map<File, MavenProject> basedirMap = MavenSessionUtils.getBasedirMap(getSortedProjects(basedir));

        MavenProject project = basedirMap.get(new File(basedir, "bundle"));
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        DependencyArtifacts platform = (DependencyArtifacts) reactorProject
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);

        CustomExecutionEnvironment customProfile = new CustomExecutionEnvironment("custom",
                Arrays.asList(new SystemCapability(Type.JAVA_PACKAGE, "package.historically.not.in.jdk", "1.2.1"), //
                        new SystemCapability(Type.OSGI_EE, "OSGi/Minimum", "1.0.0"), //
                        new SystemCapability(Type.OSGI_EE, "JavaSE", "1.0.0"), // 
                        new SystemCapability(Type.OSGI_EE, "JavaSE", "1.1.0"), //
                        new SystemCapability(Type.OSGI_EE, "JavaSE", "1.2.0")));

        ModuleContainer state = resolver.newResolvedState(reactorProject, null, customProfile, platform);
        ModuleRevision bundle = state.getModule(project.getBasedir().getAbsolutePath()).getCurrentRevision();

        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(bundle);

        if (dependencies.size() > 0) {
            assertThat(dependencies.size(), is(1));
            assertThat(dependencies.get(0).module.getSymbolicName(), is(Constants.SYSTEM_BUNDLE_SYMBOLICNAME));
        }
    }

    @Test
    public void testStrictBootClasspathAccessRules() throws Exception {
        File basedir = getBasedir("projects/bootclasspath");
        Map<File, MavenProject> basedirMap = MavenSessionUtils
                .getBasedirMap(getSortedProjects(basedir, null, getBasedir("p2repo")));
        // 1. bundle importing a JRE package only
        MavenProject bundle1Project = basedirMap.get(new File(basedir, "bundle1"));
        List<DependencyEntry> bundle1Dependencies = computeDependencies(bundle1Project);
        assertEquals(1, bundle1Dependencies.size());
        DependencyEntry dependency = bundle1Dependencies.get(0);
        assertEquals(1, dependency.rules.size());
        assertEquals("javax/net/ssl/*", dependency.rules.iterator().next().getPattern());

        // 2. bundle importing both a JRE package and an OSGi framework package
        MavenProject bundle2Project = basedirMap.get(new File(basedir, "bundle2"));
        List<DependencyEntry> bundle2Dependencies = computeDependencies(bundle2Project);
        assertEquals(1, bundle2Dependencies.size());
        DependencyEntry dependencyBundle2 = bundle2Dependencies.get(0);
        Set<String> accessRules = new HashSet<>();
        for (AccessRule rule : dependencyBundle2.rules) {
            accessRules.add(rule.getPattern());
        }
        assertEquals(new HashSet<>(asList("javax/net/ssl/*", "org/osgi/framework/*")), accessRules);
    }

    private List<DependencyEntry> computeDependencies(MavenProject project) throws BundleException {
        DependencyArtifacts platform = (DependencyArtifacts) DefaultReactorProject.adapt(project)
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        ModuleContainer state = resolver.newResolvedState(DefaultReactorProject.adapt(project), null,
                ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.4", null, null, new SilentLog()), platform);
        ModuleRevision bundle = state.getModule(project.getBasedir().getAbsolutePath()).getCurrentRevision();
        return dependencyComputer.computeDependencies(bundle);
    }

    private List<DependencyEntry> computeDependenciesIgnoringEE(MavenProject project) throws BundleException {
        DependencyArtifacts platform = (DependencyArtifacts) DefaultReactorProject.adapt(project)
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        ModuleContainer state = resolver.newResolvedState(DefaultReactorProject.adapt(project), null, null, platform);
        ModuleRevision bundle = state.getModule(project.getBasedir().getAbsolutePath()).getCurrentRevision();
        return dependencyComputer.computeDependencies(bundle);
    }

    @Test
    public void testAccessRules() throws Exception {
        File basedir = getBasedir("projects/accessrules");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(4);
        assertEquals("p002", project.getName());
        List<DependencyEntry> dependencies = computeDependencies(project);
        assertEquals(3, dependencies.size());
        assertArrayEquals(new String[] { "p001/*" }, getAccessRulePatterns(dependencies, "p001"));
        assertArrayEquals(new String[] { "p003/*" }, getAccessRulePatterns(dependencies, "p003"));
        assertArrayEquals(new String[] { "p004/*" }, getAccessRulePatterns(dependencies, "p004"));
    }

    @Test
    public void testReexportAccessRules() throws Exception {
        File basedir = getBasedir("projects/reexport");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(4);
        assertEquals("p002", project.getName());
        List<DependencyEntry> dependencies = computeDependencies(project);
        assertEquals(3, dependencies.size());
        assertArrayEquals(new String[] { "p001/*" }, getAccessRulePatterns(dependencies, "p001"));
        // next one should be accessible because p001 reexports
        assertArrayEquals(new String[] { "p003/*" }, getAccessRulePatterns(dependencies, "p003"));
        assertArrayEquals(new String[] { "p004/*" }, getAccessRulePatterns(dependencies, "p004"));
    }

    @Test
    public void testFragments() throws Exception {
        File basedir = getBasedir("projects/eeProfile.resolution.fragments");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject jface = projects.get(3);
        assertEquals("org.eclipse.jface.databinding", jface.getArtifactId());
        Collection<DependencyEntry> deps = computeDependenciesIgnoringEE(jface);
        assertTrue(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("org.eclipse.swt.gtk.linux.x86")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .filter(accessRule -> accessRule.getPattern().startsWith("org/eclipse/swt/graphics")) //
                .findAny() //
                .isPresent());
    }

    @Test
    public void testFragmentsImportClassProvidedByFragmentFromPackageExportedByHost() throws Exception {
        File basedir = getBasedir("projects/fragment-import-class-provided-by-fragment-from-package-exported-by-host");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject bundle2 = projects.get(2);
        assertEquals("bundle2", bundle2.getArtifactId());
        Collection<DependencyEntry> deps = computeDependenciesIgnoringEE(bundle2);
        assertThat(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("bundle1.fragment")) //
                .flatMap(entry -> entry.rules.stream()) //
                .map(rule -> rule.getPattern()) //
                .collect(Collectors.toList()), //
                hasItem("bundle1/*"));
    }

    @Test
    public void testFragmentSplitPackage() throws Exception {
        File basedir = getBasedir("projects/fragment-split-package");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject bundleTest = projects.get(3);
        assertEquals("bundle.tests", bundleTest.getArtifactId());
        Collection<DependencyEntry> deps = computeDependencies(bundleTest);
        assertTrue(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("bundle")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .filter(accessRule -> accessRule.getPattern().startsWith("split")) //
                .findAny() //
                .isPresent());
        assertTrue(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("fragment")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .filter(accessRule -> accessRule.getPattern().startsWith("split")) //
                .findAny() //
                .isPresent());
    }

    @Test
    public void testFragmentSplitPackageMandatory() throws Exception {
        File basedir = getBasedir("projects/fragment-split-mandatory");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject bundleTest = projects.get(3);
        assertEquals("bundle.tests", bundleTest.getArtifactId());
        Collection<DependencyEntry> deps = computeDependencies(bundleTest);
        assertTrue(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("bundle")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .filter(accessRule -> accessRule.getPattern().startsWith("split")) //
                .findAny() //
                .isPresent());
        assertTrue(deps.stream().filter(entry -> entry.module.getSymbolicName().equals("fragment")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .filter(accessRule -> accessRule.getPattern().startsWith("split")) //
                .findAny() //
                .isPresent());
    }

    @Test
    public void testImportVsRequire() throws Exception {
        File basedir = getBasedir("projects/importVsRequire");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject bundleTest = projects.get(2);
        assertEquals("A", bundleTest.getArtifactId());
        Collection<DependencyEntry> deps = computeDependencies(bundleTest);
        Collection<String> patterns = deps.stream().filter(entry -> entry.module.getSymbolicName().equals("B")) //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .map(AccessRule::getPattern) //
                .collect(Collectors.toSet());
        assertTrue(patterns.stream().anyMatch(pattern -> pattern.startsWith("b1")));
        assertTrue(patterns.stream().anyMatch(pattern -> pattern.startsWith("b2")));
    }

    @Test
    public void testDeepReexportBundle() throws Exception {
        File basedir = getBasedir("projects/deepReexport");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject bundleTest = projects.get(4);
        assertEquals("D", bundleTest.getArtifactId());
        Collection<DependencyEntry> deps = computeDependencies(bundleTest);
        Collection<String> patterns = deps.stream() //
                .flatMap(entry -> entry.rules.stream()) //
                .filter(accessRule -> !accessRule.isDiscouraged()) //
                .map(AccessRule::getPattern) //
                .collect(Collectors.toSet());
        assertTrue(patterns.stream().anyMatch(pattern -> pattern.startsWith("a")));
        assertTrue(patterns.stream().anyMatch(pattern -> pattern.startsWith("b")));
        assertTrue(patterns.stream().anyMatch(pattern -> pattern.startsWith("c")));
    }

    private String[] getAccessRulePatterns(List<DependencyEntry> dependencies, String moduleName) {
        String[] p001accessRulesPatterns = dependencies.stream()
                .filter(dep -> dep.module.getSymbolicName().equals(moduleName)) //
                .flatMap(dep -> dep.rules.stream()) //
                .map(AccessRule::getPattern) //
                .toArray(String[]::new);
        return p001accessRulesPatterns;
    }

    @Test
    public void testFragmentRequiredBundle() throws Exception {
        File basedir = getBasedir("projects/fragment");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject fragment = projects.stream().filter(p -> p.getArtifactId().equals("fragment")).findAny().get();
        Collection<DependencyEntry> deps = computeDependencies(fragment);
        assertTrue(deps.stream().filter(dep -> dep.module.getSymbolicName().equals("dep")) //
                .flatMap(dep -> dep.rules.stream()) //
                .filter(rule -> !rule.isDiscouraged()) //
                .map(AccessRule::getPattern) //
                .anyMatch(pack -> pack.startsWith("pack/")));
    }
}
