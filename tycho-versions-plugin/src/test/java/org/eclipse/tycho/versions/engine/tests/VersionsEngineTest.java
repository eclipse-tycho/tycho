/*******************************************************************************
 * Copyright (c) 2008, 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *     Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertBundleManifest;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertCategoryXml;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertFeatureXml;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertP2IuXml;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertPom;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertProductFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.tycho.testing.TestUtil;
import org.eclipse.tycho.versions.engine.IllegalVersionChangeException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.engine.VersionsEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@PlexusTest
public class VersionsEngineTest {
    @Inject
    private VersionsEngine engine;
    @Inject
    private ProjectMetadataReader reader;

    @Test
    public void testSimple() throws Exception {
        File basedir = TestUtil.getBasedir("projects/simple");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("simple", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertBundleManifest(basedir);
    }

    @Test
    public void testExportPackage() throws Exception {
        File basedir = TestUtil.getBasedir("projects/exportpackage");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("exportpackage", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertBundleManifest(basedir);
    }

    @Test
    public void testExportPackageNoBump() throws Exception {
        File basedir = TestUtil.getBasedir("projects/exportpackage-nobump");

        VersionsEngine engine = newEngine(basedir);
        engine.setUpdatePackageVersions(false);
        engine.addVersionChange("exportpackage-nobump", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertBundleManifest(basedir);
    }

    @Test
    public void testMultimodule() throws Exception {
        File basedir = TestUtil.getBasedir("projects/multimodule");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "feature01"));
        assertFeatureXml(new File(basedir, "feature01"));

        assertPom(new File(basedir, "feature02"));
        assertFeatureXml(new File(basedir, "feature02"));

        assertPom(new File(basedir, "feature03"));
        assertFeatureXml(new File(basedir, "feature03"));

        assertPom(new File(basedir, "product"));
        assertProductFile(new File(basedir, "product"), "product.product");

        assertPom(new File(basedir, "repository"));
        assertCategoryXml(new File(basedir, "repository"));
        assertProductFile(new File(basedir, "repository"), "product.product");
        assertProductFile(new File(basedir, "repository"), "differentversion.product");

        assertPom(new File(basedir, "repository-product-only"));
        assertProductFile(new File(basedir, "repository-product-only"), "product2.product");

        assertPom(new File(basedir, "iu"));
        assertP2IuXml(new File(basedir, "iu"));

    }

    @Test
    public void testUpdateVersionRanges() throws Exception {
        File basedir = TestUtil.getBasedir("projects/versionranges");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.setUpdateVersionRangeMatchingBounds(true);
        engine.apply();

        assertBundleManifest(new File(basedir, "bundle1"));

        assertBundleManifest(new File(basedir, "bundle2"));

        assertBundleManifest(new File(basedir, "bundle3"));

        assertBundleManifest(new File(basedir, "fragment"));

    }

    @Test
    public void testProfile() throws Exception {
        File basedir = TestUtil.getBasedir("projects/profile");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle01"));
        assertBundleManifest(new File(basedir, "bundle01"));

        assertPom(new File(basedir, "bundle02"));
        assertBundleManifest(new File(basedir, "bundle02"));
    }

    @Test
    public void testAggregator() throws Exception {
        File basedir = TestUtil.getBasedir("projects/aggregator");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("aggregator", "1.0.1.qualifier");
        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "parent"));

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "detached"));
        assertBundleManifest(new File(basedir, "detached"));
    }

    @Test
    public void testDependencySimple() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencysimple");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("someproject", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "someproject"));
    }

    @Test
    public void testDependencyOtherVersion() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencyotherversion");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("someproject", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "someproject"));
    }

    @Test
    public void testDependencyManagmentSimple() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencymanagementsimple");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("someproject", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "someproject"));
    }

    @Test
    public void testDependencyManagmentOtherVersion() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencymanagementotherversion");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("someproject", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "someproject"));
    }

    @Test
    public void testDeepNesting() throws Exception {
        File basedir = TestUtil.getBasedir("projects/deepnesting");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "child"));

        assertPom(new File(basedir, "child/grandchild"));

        assertPom(new File(basedir, "child/grandchild/bundle"));
        assertBundleManifest(new File(basedir, "child/grandchild/bundle"));
    }

    @Test
    public void testDeepNestingInverseOrder() throws Exception {
        File basedir = TestUtil.getBasedir("projects/deepnesting");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("child", "1.0.1.qualifier");
        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "child"));

        assertPom(new File(basedir, "child/grandchild"));

        assertPom(new File(basedir, "child/grandchild/bundle"));
        assertBundleManifest(new File(basedir, "child/grandchild/bundle"));
    }

    @Test
    public void testExplicitVersion() throws Exception {
        File basedir = TestUtil.getBasedir("projects/exlicitversion");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "otherversion"));
        assertBundleManifest(new File(basedir, "otherversion"));

        assertPom(new File(basedir, "sameversion"));
        assertBundleManifest(new File(basedir, "sameversion"));
    }

    @Test
    public void testPomDependencyNoVersion() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencynoversion");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("testmodule", "4.8");
        engine.apply();

        assertPom(new File(basedir, "module"));
    }

    @Test
    public void testWrongSnapshotVersion() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Versions.assertIsOsgiVersion("1.2.3_SNAPSHOT"));
    }

    @Test
    public void testAssertOsgiVersion() {
        Versions.assertIsOsgiVersion("1.2.3.qualifier");
    }

    @Test
    public void testBuildPluginManagement() throws Exception {
        File basedir = TestUtil.getBasedir("projects/pluginmanagement");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertPom(new File(basedir, "plugin"));
        assertPom(new File(basedir, "jar"));
    }

    @Test
    public void testPomProperties() throws Exception {
        File basedir = TestUtil.getBasedir("projects/pomproperties");

        VersionsEngine engine = newEngine(basedir);

        engine.addPropertyChange("pomproperties", "p1", "changed");
        engine.apply();

        assertPom(basedir);
    }

    @ParameterizedTest
    @ValueSource(strings = { "bundle", "feature", "product", "repository" })
    public void testNonOsgiVersionOsgiProject(String value) throws Exception {
        assertNonOsgiVersionOsgiProject(value);
    }

    private void assertNonOsgiVersionOsgiProject(String artifactId) throws Exception {
        File basedir = TestUtil.getBasedir("projects/nonosgiversion/" + artifactId);

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange(artifactId, "1.0.1-01");
        IllegalVersionChangeException e = assertThrows(IllegalVersionChangeException.class, () -> engine.apply());
        // not a valid osgi version
        assertEquals(1, e.getErrors().size());
    }

    @Test
    public void testNonOsgiVersionNonOsgiProject() throws Exception {
        File basedir = TestUtil.getBasedir("projects/nonosgiversion/maven");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("maven", "1.0.1-01");
        engine.apply();
    }

    @Test
    public void testBuildPluginNoGroupId() throws Exception {
        File basedir = TestUtil.getBasedir("projects/buildpluginnogroupid");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("buildpluginnogroupid", "1.0.1-01");
        engine.apply();
    }

    @Test
    public void testProfileNoId() throws Exception {
        File basedir = TestUtil.getBasedir("projects/profilenoid");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("profilenoid", "1.0.1-01");
        engine.apply();
    }

    @Test
    public void testTargetPlatform() throws Exception {
        File basedir = TestUtil.getBasedir("projects/targetplatform");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("parent", "0.2.0.qualifier");
        engine.apply();

        assertPom(basedir);

        assertPom(new File(basedir, "bundle01"));
        assertBundleManifest(new File(basedir, "bundle01"));

        assertPom(new File(basedir, "targetplatform"));
    }

    private VersionsEngine newEngine(File basedir) throws Exception {

        reader.addBasedir(basedir, true);

        engine.setProjects(reader.getProjects());

        return engine;
    }
}
