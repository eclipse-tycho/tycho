/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.eclipse.tycho.surefire.provider.impl.AbstractJUnitProviderTest.classPath;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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
    private static final String JUNIT47_FRAGMENT = "org.eclipse.tycho.surefire.junit47";
    private static final String BOOTER_ARTIFACTID = "org.eclipse.tycho.surefire.osgibooter";
    private static final String UI_HARNESS_FRAGMENT = "org.eclipse.tycho.surefire.uiharness";

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
        TestFrameworkProvider provider = providerHelper.selectProvider(
                classPath("org.junit:3.8.2", "org.junit4:4.8.1"), providerProperties, null);
        assertEquals(JUnit47Provider.class, provider.getClass());
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

    public void testSelectWithNonExistingHint() {
        try {
            providerHelper.selectProvider(classPath(), new Properties(), "NON_EXISTING");
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testNoProviderFound() {
        try {
            providerHelper.selectProvider(classPath("foo:1.0", "test:2.0"), new Properties(), null);
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testParallelModeNotSupported() {
        try {
            Properties providerProperties = new Properties();
            providerProperties.setProperty("parallel", "methods");
            providerHelper.selectProvider(classPath("org.junit:4.6"), providerProperties, null);
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testMultipleProviderTypesFound() throws Exception {
        TestFrameworkProvider anotherProvider = new TestFrameworkProvider() {

            public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
                return true;
            }

            public Version getVersion() {
                return new Version("1.0");
            }

            public String getType() {
                return "another_test_fwk";
            }

            public String getSurefireProviderClassName() {
                return "a.nother.test.framework.Provider";
            }

            public List<Dependency> getRequiredBundles() {
                return emptyList();
            }

            public List<String> getRequiredBundleSymbolicNames() {
                return emptyList();
            }
        };
        PlexusContainer container = getContainer();
        container.addComponent(anotherProvider, TestFrameworkProvider.class, "another_test_fwk");
        ProviderHelper providerSelector = container.lookup(ProviderHelper.class);
        try {
            providerSelector.selectProvider(classPath("org.junit:4.8.1"), new Properties(), null);
            fail();
        } catch (MojoExecutionException e) {
            // expected
        } finally {
            container.release(anotherProvider);
        }
    }

    public void testFilterTestFrameworkBundlesNotFound() {
        try {
            providerHelper.filterTestFrameworkBundles(new JUnit3Provider(), false,
                    asList(createMockArtifact("test", "test")));
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testFilterTestFrameworkBundlesJUnit3() throws MojoExecutionException {
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = providerHelper.filterTestFrameworkBundles(
                new JUnit3Provider(), false,
                asList(booterArtifact(), junit3Artifact(), junit4Artifact(), createMockArtifact("foo", "bar")));
        assertEquals(2, junitSurefireBundles.size());
        Set<String> fileNames = new HashSet<String>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        HashSet<String> expectedFileNames = new HashSet<String>(asList(TYCHO_GROUPID + "_" + BOOTER_ARTIFACTID,
                TYCHO_GROUPID + "_" + JUNIT3_FRAGMENT));
        assertEquals(expectedFileNames, fileNames);
    }

    public void testFilterTestFrameworkBundlesJUnit4() throws MojoExecutionException {
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = providerHelper.filterTestFrameworkBundles(
                new JUnit4Provider(),
                false,
                asList(booterArtifact(), junit3Artifact(), junit4Artifact(), uiHarnessArtifact(),
                        createMockArtifact("foo", "bar")));
        assertEquals(2, junitSurefireBundles.size());
        Set<String> fileNames = new HashSet<String>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        HashSet<String> expectedFileNames = new HashSet<String>(asList(TYCHO_GROUPID + "_" + BOOTER_ARTIFACTID,
                TYCHO_GROUPID + "_" + JUNIT4_FRAGMENT));
        assertEquals(expectedFileNames, fileNames);
    }

    public void testFilterTestFrameworkBundlesUIHarness() throws MojoExecutionException {
        assertHasUIHarness(new JUnit4Provider());
        assertHasUIHarness(new JUnit3Provider());
        assertHasUIHarness(new JUnit47Provider());
    }

    private void assertHasUIHarness(TestFrameworkProvider provider) throws MojoExecutionException {
        List<Artifact> mockPluginArtifacts = asList(booterArtifact(), junit3Artifact(), junit4Artifact(),
                junit47Artifact(), uiHarnessArtifact(), createMockArtifact("foo", "bar"));
        final boolean useUIHarness = true;
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = providerHelper.filterTestFrameworkBundles(
                provider, useUIHarness, mockPluginArtifacts);
        Set<String> fileNames = new HashSet<String>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        assertTrue(fileNames.contains(TYCHO_GROUPID + "_" + UI_HARNESS_FRAGMENT));
    }

    public void testGetSymbolicNames() throws MojoExecutionException {
        List<String> symbolicNames = providerHelper.getSymbolicNames(Collections.singleton(createMockArtifact("foo",
                "bar", new File("src/test/resources/org.junit_3.8.2.v20090203-1005"))));
        assertEquals(1, symbolicNames.size());
        assertEquals("org.junit", symbolicNames.get(0));
    }

    private org.apache.maven.artifact.Artifact junit3Artifact() {
        return createMockArtifact(TYCHO_GROUPID, JUNIT3_FRAGMENT);
    }

    private org.apache.maven.artifact.Artifact junit4Artifact() {
        return createMockArtifact(TYCHO_GROUPID, JUNIT4_FRAGMENT);
    }

    private org.apache.maven.artifact.Artifact junit47Artifact() {
        return createMockArtifact(TYCHO_GROUPID, JUNIT47_FRAGMENT);
    }

    private org.apache.maven.artifact.Artifact uiHarnessArtifact() {
        return createMockArtifact(TYCHO_GROUPID, UI_HARNESS_FRAGMENT);
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
