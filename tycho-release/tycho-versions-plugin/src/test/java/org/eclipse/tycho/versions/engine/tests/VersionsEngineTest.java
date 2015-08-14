/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *     Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import java.io.File;

import org.eclipse.tycho.testing.TestUtil;
import org.eclipse.tycho.versions.engine.IllegalVersionChangeException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.engine.VersionsEngine;

public class VersionsEngineTest extends AbstractVersionChangeTest {
    public void testSimple() throws Exception {
        File basedir = TestUtil.getBasedir("projects/simple");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("simple", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertBundleManifest(basedir);
    }

    public void testExportPackage() throws Exception {
        File basedir = TestUtil.getBasedir("projects/exportpackage");

        VersionsEngine engine = newEngine(basedir);
        engine.addVersionChange("exportpackage", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertBundleManifest(basedir);
    }

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

        assertPom(new File(basedir, "site"));
        assertSiteXml(new File(basedir, "site"));

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

    public void testPomDependencyNoVersion() throws Exception {
        File basedir = TestUtil.getBasedir("projects/dependencynoversion");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("testmodule", "4.8");
        engine.apply();

        assertPom(new File(basedir, "module"));
    }

    public void testWrongSnapshotVersion() throws Exception {
        try {
            Versions.assertIsOsgiVersion("1.2.3_SNAPSHOT");
            fail("invalid version accepted");
        } catch (NumberFormatException e) {
            // thrown by equinox <3.8M5
        } catch (IllegalArgumentException e) {
            // thrown by equinox 3.8M5
        }
    }

    public void testAssertOsgiVersion() {
        Versions.assertIsOsgiVersion("1.2.3.qualifier");
    }

    public void testBuildPluginManagement() throws Exception {
        File basedir = TestUtil.getBasedir("projects/pluginmanagement");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("parent", "1.0.1.qualifier");
        engine.apply();

        assertPom(basedir);
        assertPom(new File(basedir, "plugin"));
        assertPom(new File(basedir, "jar"));
    }

    public void testPomProperties() throws Exception {
        File basedir = TestUtil.getBasedir("projects/pomproperties");

        VersionsEngine engine = newEngine(basedir);

        engine.addPropertyChange("pomproperties", "p1", "changed");
        engine.apply();

        assertPom(basedir);
    }

    public void testNonOsgiVersionOsgiProject() throws Exception {
        assertNonOsgiVersionOsgiProject("bundle");
        assertNonOsgiVersionOsgiProject("feature");
        assertNonOsgiVersionOsgiProject("product");
        assertNonOsgiVersionOsgiProject("repository");
    }

    private void assertNonOsgiVersionOsgiProject(String artifactId) throws Exception {
        File basedir = TestUtil.getBasedir("projects/nonosgiversion/" + artifactId);

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange(artifactId, "1.0.1-01");
        try {
            engine.apply();
            fail();
        } catch (IllegalVersionChangeException e) {
            // not a valid osgi version
            assertEquals(1, e.getErrors().size());
        }
    }

    public void testNonOsgiVersionNonOsgiProject() throws Exception {
        File basedir = TestUtil.getBasedir("projects/nonosgiversion/maven");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("maven", "1.0.1-01");
        engine.apply();
    }

    public void testBuildPluginNoGroupId() throws Exception {
        File basedir = TestUtil.getBasedir("projects/buildpluginnogroupid");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("buildpluginnogroupid", "1.0.1-01");
        engine.apply();
    }

    public void testProfileNoId() throws Exception {
        File basedir = TestUtil.getBasedir("projects/profilenoid");

        VersionsEngine engine = newEngine(basedir);

        engine.addVersionChange("profilenoid", "1.0.1-01");
        engine.apply();
    }

    private VersionsEngine newEngine(File basedir) throws Exception {
        VersionsEngine engine = lookup(VersionsEngine.class);
        ProjectMetadataReader reader = lookup(ProjectMetadataReader.class);

        reader.addBasedir(basedir);

        engine.setProjects(reader.getProjects());

        return engine;
    }
}
