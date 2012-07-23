/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.sonatype.aether.artifact.Artifact;

public class ProviderSelectorTest extends PlexusTestCase {

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final String JUNIT3_FRAGMENT = "org.eclipse.tycho.surefire.junit";
    private static final String JUNIT4_FRAGMENT = "org.eclipse.tycho.surefire.junit4";
    private static final String BOOTER_ARTIFACTID = "org.eclipse.tycho.surefire.osgibooter";

    private ProviderSelector selector;

    @Override
    protected void setUp() throws Exception {
        this.selector = getContainer().lookup(ProviderSelector.class);
    }

    public void testSelectJunit3() throws Exception {
        TestFrameworkProvider provider = selector.selectProvider(classPath("org.junit:3.8"), new Properties(), null);
        assertEquals(JUnit3Provider.class, provider.getClass());
    }

    public void testSelectJunit4() throws Exception {
        TestFrameworkProvider provider = selector.selectProvider(classPath("org.junit:4.8.1"), new Properties(), null);
        assertEquals(JUnit4Provider.class, provider.getClass());
    }

    public void testSelectJunit47() throws Exception {
        Properties providerProperties = new Properties();
        providerProperties.setProperty("parallel", "classes");
        TestFrameworkProvider provider = selector.selectProvider(classPath("org.junit:3.8.2", "org.junit4:4.8.1"),
                providerProperties, null);
        assertEquals(JUnit47Provider.class, provider.getClass());
    }

    public void testSelectJunit4WithJunit3Present() throws Exception {
        TestFrameworkProvider provider = selector.selectProvider(classPath("org.junit:3.8.1", "org.junit:4.8.1"),
                new Properties(), null);
        assertEquals(JUnit4Provider.class, provider.getClass());
    }

    public void testForceJunit3WithHint() throws Exception {
        TestFrameworkProvider provider = selector.selectProvider(classPath("org.junit:3.8.1", "org.junit:4.8.1"),
                new Properties(), "junit3");
        assertEquals(JUnit3Provider.class, provider.getClass());
    }

    public void testSelectWithNonExistingHint() {
        try {
            selector.selectProvider(classPath(), new Properties(), "NON_EXISTING");
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testNoProviderFound() {
        try {
            selector.selectProvider(classPath("foo:1.0", "test:2.0"), new Properties(), null);
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testParallelModeNotSupported() {
        try {
            Properties providerProperties = new Properties();
            providerProperties.setProperty("parallel", "methods");
            selector.selectProvider(classPath("org.junit:4.6"), providerProperties, null);
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

            public List<Artifact> getRequiredBundles() {
                return emptyList();
            }
        };
        PlexusContainer container = getContainer();
        container.addComponent(anotherProvider, TestFrameworkProvider.class, "another_test_fwk");
        ProviderSelector providerSelector = container.lookup(ProviderSelector.class);
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
            selector.filterTestFrameworkBundles(new JUnit3Provider(), asList(createMockArtifact("test", "test")));
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    public void testFilterTestFrameworkBundlesJUnit3() throws MojoExecutionException {
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = selector.filterTestFrameworkBundles(
                new JUnit3Provider(),
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
        Set<org.apache.maven.artifact.Artifact> junitSurefireBundles = selector.filterTestFrameworkBundles(
                new JUnit4Provider(),
                asList(booterArtifact(), junit3Artifact(), junit4Artifact(), createMockArtifact("foo", "bar")));
        assertEquals(2, junitSurefireBundles.size());
        Set<String> fileNames = new HashSet<String>();
        for (org.apache.maven.artifact.Artifact artifact : junitSurefireBundles) {
            fileNames.add(artifact.getFile().getName());
        }
        HashSet<String> expectedFileNames = new HashSet<String>(asList(TYCHO_GROUPID + "_" + BOOTER_ARTIFACTID,
                TYCHO_GROUPID + "_" + JUNIT4_FRAGMENT));
        assertEquals(expectedFileNames, fileNames);
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
        org.apache.maven.artifact.Artifact artifact = new DefaultArtifact(groupId, artifactId, "1.0", "compile", "jar",
                "classifier", null);
        artifact.setFile(new File(groupId + "_" + artifactId));
        return artifact;
    }
}
