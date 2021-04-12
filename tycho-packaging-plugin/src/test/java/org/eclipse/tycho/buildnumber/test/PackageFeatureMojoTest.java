/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.IOUtil;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.packaging.PackageFeatureMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class PackageFeatureMojoTest extends AbstractTychoMojoTestCase {

    public void testLicenseFeature() throws Exception {
        File basedir = getBasedir("projects/licenseFeature/feature");
        File platform = new File("src/test/resources/projects/licenseFeature/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "licenseFeature.feature");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageFeatureMojo mojo = (PackageFeatureMojo) lookupMojo("package-feature", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "finalName", "feature");

        mojo.execute();

        ZipFile zip = new ZipFile(new File(basedir, "target/feature.jar"));
        try {
            // igorf: input streams are closed by zip.close() at the end, sloppy but should work 

            // all bin.includes files from license features are included
            assertNotNull(zip.getEntry("file-license.txt"));
            assertNull(zip.getEntry("file-unlicense.txt"));

            // do not leak build.properties into 'this' feature
            assertNull(zip.getEntry("build.properties"));

            // license feature id/version are stripped off
            Feature feature = Feature.read(zip.getInputStream(zip.getEntry(Feature.FEATURE_XML)));
            assertNull(feature.getLicenseFeature());
            assertNull(feature.getLicenseFeatureVersion());
            assertEquals("http://www.foo.bar", feature.getLicenseURL());
            assertEquals("This is the license", feature.getLicense().trim());

            // feature.properties merged
            Properties p = new Properties();
            p.load(zip.getInputStream(zip.getEntry("feature.properties")));
            assertEquals("test property value", p.getProperty("test"));
            assertEquals("license test property value", p.getProperty("license-test"));

            // when present both in 'this' and license feature, files from 'this' feature are included
            assertEquals("file.txt contents", IOUtil.toString(zip.getInputStream(zip.getEntry("file.txt"))));
        } finally {
            zip.close();
        }
    }

    public void testAddMavenDescriptorNotAddedPerDefault() throws Exception {
        File basedir = getBasedir("projects/addMavenDescriptor/featureDefault/");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "featureDefault");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageFeatureMojo mojo = (PackageFeatureMojo) lookupMojo("package-feature", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);

        mojo.execute();

        ZipFile zip = new ZipFile(new File(basedir, "target/featureDefault.jar"));
        try {
            ZipEntry entry = zip.getEntry("META-INF/maven");
            assertNull("No 'META-INF/maven/' entry must be in the feature.jar!", entry);
        } finally {
            zip.close();
        }
    }

    public void testAddMavenDescriptorSetToTrue() throws Exception {
        File basedir = getBasedir("projects/addMavenDescriptor/featureForcedToTrue");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "featureForcedToTrue");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageFeatureMojo mojo = (PackageFeatureMojo) lookupMojo("package-feature", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);

        mojo.execute();

        ZipFile zip = new ZipFile(new File(basedir, "target/featureForcedToTrue.jar"));
        try {
            ZipEntry entry = zip.getEntry("META-INF/maven");
            assertNotNull("There must be a 'META-INF/maven/' entry in the feature.jar!", entry);
        } finally {
            zip.close();
        }
    }
}
