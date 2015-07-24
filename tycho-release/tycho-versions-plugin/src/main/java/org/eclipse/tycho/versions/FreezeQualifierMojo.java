/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoProperties;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.osgi.framework.Version;

/**
 * Freeze the current build qualifier (as computed by tycho-packaging-plugin:build-qualifier) by
 * hardcoding it in pom.xml, MANIFEST.MF and feature.xml. Only versions of projects with packaging
 * type <tt>eclipse-plugin</tt>, <tt>eclipse-test-plugin</tt> or <tt>eclipse-feature</tt> which have
 * a 4-digit OSGi version ending with literal &quot;.qualifier&quot; will be changed. For a given
 * artifact, its maven version (in pom.xml) and its OSGi version (in MANIFEST.MF or feature.xml)
 * will be hardcoded to exactly the same value.
 * 
 * Note that at least lifecycle phase &quot;validate&quot; must be run prior to executing this goal.
 * This ensures the current build qualifier is computed.<br/>
 * 
 * This goal can be useful as a preparation step before doing a release build to ensure a
 * reproducible build.
 *
 */
@Mojo(name = "freeze-qualifier", requiresDirectInvocation = true)
public class FreezeQualifierMojo extends AbstractVersionsMojo {

    private static final String QUALIFIER_SUFFIX = ".qualifier";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    private static final HashSet<String> SUPPORTED_PACKAGING_TYPES = new HashSet<>(
            Arrays.asList(PackagingType.TYPE_ECLIPSE_PLUGIN, PackagingType.TYPE_ECLIPSE_TEST_PLUGIN,
                    PackagingType.TYPE_ECLIPSE_FEATURE));
    private static final Pattern EXPECTED_OSGI_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.qualifier");
    private static final Pattern EXPECTED_MAVEN_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-SNAPSHOT");

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    /**
     * Optional version suffix to be appended to the computed build qualifier value.<br/>
     * Examples: <tt>-M1</tt>, <tt>-RELEASE</tt>
     */
    @Parameter(property = "versionSuffix")
    private String versionSuffix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String packaging = project.getPackaging();
        if (!SUPPORTED_PACKAGING_TYPES.contains(packaging)) {
            // skip
            return;
        }
        String qualifier = project.getProperties().getProperty(TychoProperties.BUILD_QUALIFIER);
        if (qualifier == null) {
            throw new MojoFailureException("project property ${" + TychoProperties.BUILD_QUALIFIER
                    + "} not set. At least lifecycle phase 'validate' must be executed before this goal");
        }
        if (versionSuffix != null) {
            qualifier = qualifier + versionSuffix;
        }
        switch (packaging) {
        case PackagingType.TYPE_ECLIPSE_PLUGIN:
        case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
            updateVersionQualiferInManifest(qualifier);
            break;
        case PackagingType.TYPE_ECLIPSE_FEATURE:
            updateVersionQualiferInFeatureXml(qualifier);
            break;
        }
        replaceSnapshotVersionSuffixInPom(qualifier);
    }

    private void replaceSnapshotVersionSuffixInPom(String qualifier) throws MojoExecutionException {
        File pomFile = project.getFile();
        if (!pomFile.isFile() || !"pom.xml".equals(pomFile.getName())) {
            // pom-less build - nothing to do
            return;
        }
        try {
            MutablePomFile pom = MutablePomFile.read(pomFile);
            String oldVersion = pom.getVersion();
            if (!matchesMavenVersionPattern(oldVersion, pomFile)) {
                return;
            }
            String newVersion = replaceSnapshotSuffix(oldVersion, qualifier);
            pom.setVersion(newVersion);
            MutablePomFile.write(pom, pomFile);
            logVersionChanged(oldVersion, newVersion, pomFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private void updateVersionQualiferInManifest(String qualifier) throws MojoExecutionException {
        File manifestFile = new File(project.getBasedir(), "META-INF/MANIFEST.MF");
        try {
            MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
            String oldVersion = manifest.getVersion();
            if (!matchesOsgiVersionPattern(oldVersion, manifestFile)) {
                return;
            }
            String newVersion = replaceQualifierSuffix(oldVersion, qualifier);
            manifest.setVersion(newVersion);
            MutableBundleManifest.write(manifest, manifestFile);
            logVersionChanged(oldVersion, newVersion, manifestFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private void updateVersionQualiferInFeatureXml(String qualifier) throws MojoExecutionException {
        File featureXmlFile = new File(project.getBasedir(), "feature.xml");
        try {
            Feature feature = Feature.read(featureXmlFile);
            String oldVersion = feature.getVersion();
            if (!matchesOsgiVersionPattern(oldVersion, featureXmlFile)) {
                return;
            }
            String newVersion = replaceQualifierSuffix(oldVersion, qualifier);
            feature.setVersion(newVersion);
            Feature.write(feature, featureXmlFile);
            logVersionChanged(oldVersion, newVersion, featureXmlFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean matchesOsgiVersionPattern(String version, File source) {
        boolean isValid = EXPECTED_OSGI_VERSION_PATTERN.matcher(version).matches();
        if (!isValid) {
            getLog().warn(String.format(
                    "Version '%s' in '%s' does not match pattern 'major.minor.micro.qualifier' - skipping.", version,
                    source));
        }
        return isValid;
    }

    private boolean matchesMavenVersionPattern(String version, File source) {
        boolean isValid = EXPECTED_MAVEN_VERSION_PATTERN.matcher(version).matches();
        if (!isValid) {
            getLog().warn(String.format(
                    "Version '%s' in '%s' does not match pattern 'major.minor.micro-SNAPSHOT' - skipping.", version,
                    source));
        }
        return isValid;
    }

    private void logVersionChanged(String oldVersion, String newVersion, File file) {
        getLog().info(
                String.format("Changed version: '%s' -> '%s' in %s", oldVersion, newVersion, file.getAbsolutePath()));
    }

    private static String replaceQualifierSuffix(String version, String newQualifier) throws MojoExecutionException {
        String newVersion = version.substring(0, version.length() - QUALIFIER_SUFFIX.length()) + "." + newQualifier;
        validateOsgiVersion(newVersion);
        return newVersion;
    }

    private static void validateOsgiVersion(String version) throws MojoExecutionException {
        try {
            Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static String replaceSnapshotSuffix(String version, String newQualifier) {
        return version.substring(0, version.length() - SNAPSHOT_SUFFIX.length()) + "." + newQualifier;
    }

}
