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

import static java.util.Collections.emptyList;
import static org.eclipse.tycho.surefire.provider.impl.AbstractJUnitProviderTest.classPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

public class ProviderHelperTest extends TychoPlexusTestCase {

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final String JUNIT3_FRAGMENT = "org.eclipse.tycho.surefire.junit";
    private static final String JUNIT4_FRAGMENT = "org.eclipse.tycho.surefire.junit4";
    private static final String BOOTER_ARTIFACTID = "org.eclipse.tycho.surefire.osgibooter";

    private ProviderHelper providerHelper;

    @Before
    public void setUpTest() throws Exception {
        this.providerHelper = lookup(ProviderHelper.class);
    }

    @Test
    public void testSelectJunit47() throws Exception {
        Properties providerProperties = new Properties();
        providerProperties.setProperty("parallel", "classes");
        TestFrameworkProvider provider = providerHelper.selectProvider(null,
                classPath("org.junit:3.8.2", "org.junit4:4.8.1"), providerProperties, null);
        assertEquals(JUnit4Provider.class, provider.getClass());
    }

    @Test
    public void testSelectJunit5() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(null, classPath("junit-jupiter-api:5.0.0"),
                new Properties(), null);
        assertEquals(JUnit5Provider.class, provider.getClass());
    }

    @Test
    public void testSelectJunit5WithJUnit4Present() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(null,
                classPath("org.junit:4.12", "junit-jupiter-api:5.0.0"), new Properties(), null);
        assertEquals(JUnit5WithVintageProvider.class, provider.getClass());
    }

    @Test
    public void testSelectTestNG() throws Exception {
        TestFrameworkProvider provider = providerHelper.selectProvider(null, classPath("org.testng:6.9.12"),
                new Properties(), null);
        assertEquals(TestNGProvider.class, provider.getClass());
    }

    @Test
    public void testSelectWithNonExistingHint() {
        assertThrows(MojoExecutionException.class,
                () -> providerHelper.selectProvider(null, classPath(), new Properties(), "NON_EXISTING"));
    }

    @Test
    public void testNoProviderFound() {
        assertThrows(MojoExecutionException.class,
                () -> providerHelper.selectProvider(null, classPath("foo:1.0", "test:2.0"), new Properties(), null));
    }

    @Test
    public void testMultipleProviderTypesFound() throws Exception {
        TestFrameworkProvider anotherProvider = new TestFrameworkProvider() {

            @Override
            public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
                    Properties surefireProperties) {
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
                    () -> providerSelector.selectProvider(null, classPath("org.junit:4.8.1"), new Properties(), null));
        } finally {
            container.release(anotherProvider);
        }
    }

    @Test
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
