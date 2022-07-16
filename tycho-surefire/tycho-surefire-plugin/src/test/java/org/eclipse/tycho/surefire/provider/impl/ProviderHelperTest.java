/*******************************************************************************
 * Copyright (c) 2012, 2016 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.eclipse.tycho.surefire.provider.impl.AbstractJUnitProviderTest.classPath;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;

public class ProviderHelperTest extends PlexusTestCase {

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final String JUNIT3_FRAGMENT = "org.eclipse.tycho.surefire.junit";
    private static final String JUNIT4_FRAGMENT = "org.eclipse.tycho.surefire.junit4";
    private static final String BOOTER_ARTIFACTID = "org.eclipse.tycho.surefire.osgibooter";

    private ProviderHelper providerHelper;

    @Override
    protected void setUp() throws Exception {
        this.providerHelper = getContainer().lookup(ProviderHelper.class);
    }

    public void testSelectJunit3() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit:3.8"), new Properties(),
                null);
        assertEquals(JUnit3Provider.class, provider.getClass());
    }

    public void testSelectJunit4() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit:4.8.1"), new Properties(),
                null);
        assertEquals(JUnit4Provider.class, provider.getClass());
    }

    public void testSelectJunit47() throws Exception {
        Properties providerProperties = new Properties();
        providerProperties.setProperty("parallel", "classes");
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit:3.8.2", "org.junit4:4.8.1"),
                providerProperties, null);
        assertEquals(JUnit47Provider.class, provider.getClass());
    }

    public void testSelectJunit5WithJUnitFromOrbit() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit.jupiter.api:5.0.0"),
                new Properties(), null);
        assertEquals(JUnit5Provider.class, provider.getClass());
    }

    public void testSelectJunit5() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("junit-jupiter-api:5.0.0"),
                new Properties(), null);
        assertEquals(JUnit5Provider.class, provider.getClass());
    }

    public void testSelectJunit5WithJUnit4Present() throws Exception {
        TestFrameworkProvider provider = providerHelper
                .selectProvider(classPath("org.junit:4.12", "org.junit.jupiter.api:5.0.0"), new Properties(), null);
        assertEquals(JUnit5Provider.class, provider.getClass());
    }

    public void testSelectJunit4WithJunit3Present() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit:3.8.1", "org.junit:4.8.1"),
                new Properties(), null);
        assertEquals(JUnit4Provider.class, provider.getClass());
    }

    public void testForceJunit3WithHint() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.junit:3.8.1", "org.junit:4.8.1"),
                new Properties(), "junit3");
        assertEquals(JUnit3Provider.class, provider.getClass());
    }

    public void testSelectTestNG() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(classPath("org.testng:6.9.12"), new Properties(),
                null);
        assertEquals(TestNGProvider.class, provider.getClass());
    }

    public void testSelectWithNonExistingHint() {
        assertThrows(MojoExecutionException.class,
                () -> providerHelper.selectProvider(classPath(), new Properties(), "NON_EXISTING"));
    }

    public void testNoProviderFound() {
        assertThrows(MojoExecutionException.class,
                () -> providerHelper.selectProvider(classPath("foo:1.0", "test:2.0"), new Properties(), null));
    }

    public void testParallelModeNotSupported() {
        Properties providerProperties = new Properties();
        providerProperties.setProperty("parallel", "methods");
        assertThrows(MojoExecutionException.class,
                () -> providerHelper.selectProvider(classPath("org.junit:4.6"), providerProperties, null));
    }

    public void testMultipleProviderTypesFound() throws Exception {
        TestFrameworkProvider anotherProvider = new TestFrameworkProvider() {

            @Override
            public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
                return true;
            }

            @Override
            public Version getVersion() {
                return new Version("1.0");
            }

            @Override
            public String getType() {
                return "another_test_fwk";
            }

            @Override
            public String getSurefireProviderClassName() {
                return "a.nother.test.framework.Provider";
            }

            @Override
            public List<Dependency> getRequiredBundles() {
                return emptyList();
            }

            @Override
            public Properties getProviderSpecificProperties() {
                return new Properties();
            }
        };
        PlexusContainer container = getContainer();
        container.addComponent(anotherProvider, TestFrameworkProvider.class, "another_test_fwk");
        ProviderHelper providerSelector = container.lookup(ProviderHelper.class);
        try {
            assertThrows(MojoExecutionException.class,
                    () -> providerSelector.selectProvider(classPath("org.junit:4.8.1"), new Properties(), null));
        } finally {
            container.release(anotherProvider);
        }
    }

    public void testFilterTestFrameworkBundlesNotFound() {
        assertThrows(MojoExecutionException.class, () -> providerHelper.filterTestFrameworkBundles(new JUnit3Provider(),
                asList(createMockArtifact("test", "test"))));
    }

    public void testFilterTestFrameworkBundlesJUnit3() throws MojoExecutionException {
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = providerHelper.filterTestFrameworkBundles(
                new JUnit3Provider(),
                asList(booterArtifact(), junit3Artifact(), junit4Artifact(), createMockArtifact("foo", "bar")));
        assertEquals(2, junitSurefireBundles.size());
        Set<String> fileNames = new HashSet<>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        HashSet<String> expectedFileNames = new HashSet<>(
                asList(TYCHO_GROUPID + "_" + BOOTER_ARTIFACTID, TYCHO_GROUPID + "_" + JUNIT3_FRAGMENT));
        assertEquals(expectedFileNames, fileNames);
    }

    public void testFilterTestFrameworkBundlesJUnit4() throws MojoExecutionException {
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = providerHelper.filterTestFrameworkBundles(
                new JUnit4Provider(),
                asList(booterArtifact(), junit3Artifact(), junit4Artifact(), createMockArtifact("foo", "bar")));
        assertEquals(2, junitSurefireBundles.size());
        Set<String> fileNames = new HashSet<>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        HashSet<String> expectedFileNames = new HashSet<>(
                asList(TYCHO_GROUPID + "_" + BOOTER_ARTIFACTID, TYCHO_GROUPID + "_" + JUNIT4_FRAGMENT));
        assertEquals(expectedFileNames, fileNames);
    }

    public void testGetSymbolicNames() throws MojoExecutionException {
        List<String> symbolicNames = providerHelper.getSymbolicNames(Collections.singleton(
                createMockArtifact("foo", "bar", new File("src/test/resources/org.junit_3.8.2.v20090203-1005"))));
        assertEquals(1, symbolicNames.size());
        assertEquals("org.junit", symbolicNames.get(0));
    }

    private org.apache.maven.artifact.Artifact junit3Artifact() {
        return createMockArtifact(TYCHO_GROUPID, JUNIT3_FRAGMENT);
    }

    private org.apache.maven.artifact.Artifact junit4Artifact() {
        return createMockArtifact(TYCHO_GROUPID, JUNIT4_FRAGMENT);
    }

    private org.apache.maven.artifact.Artifact booterArtifact() {
        return createMockArtifact(TYCHO_GROUPID, BOOTER_ARTIFACTID);
    }

    private org.apache.maven.artifact.Artifact createMockArtifact(String groupId, String artifactId) {
        return createMockArtifact(groupId, artifactId, null);
    }

    private org.apache.maven.artifact.Artifact createMockArtifact(String groupId, String artifactId, File file) {
        org.apache.maven.artifact.Artifact artifact = new DefaultArtifact(groupId, artifactId, "1.0", "compile", "jar",
                "classifier", null);
        artifact.setFile(file != null ? file : new File(groupId + "_" + artifactId));
        return artifact;
    }
}
