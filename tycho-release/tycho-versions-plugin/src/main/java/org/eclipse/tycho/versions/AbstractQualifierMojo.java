/*******************************************************************************
 * Copyright (c) 2016 SAP SE and others.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.osgi.framework.Version;

public abstract class AbstractQualifierMojo extends AbstractMojo {

    private static final HashSet<String> SUPPORTED_PACKAGING_TYPES = new HashSet<>(
            Arrays.asList(PackagingType.TYPE_ECLIPSE_PLUGIN, PackagingType.TYPE_ECLIPSE_TEST_PLUGIN,
                    PackagingType.TYPE_ECLIPSE_FEATURE));

    @Parameter(property = "project", readonly = true)
    protected MavenProject project;

    protected abstract String getNewOsgiVersionQualifier() throws MojoFailureException;

    protected abstract String getNewMavenVersionQualifier() throws MojoFailureException;

    protected abstract Pattern getOldOsgiVersionPattern();

    protected abstract Pattern getOldMavenVersionPattern();

    protected abstract String createNewOsgiVersion(String oldOsgiVersion, String newQualifier)
            throws MojoExecutionException;

    protected abstract String createNewPomVersion(String oldPomVersion, String newQualifier);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String packaging = project.getPackaging();
        if (!SUPPORTED_PACKAGING_TYPES.contains(packaging)) {
            // skip
            return;
        }
        String newQualifier = getNewOsgiVersionQualifier();
        switch (packaging) {
        case PackagingType.TYPE_ECLIPSE_PLUGIN:
        case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
            updateVersionQualiferInManifest(newQualifier);
            break;
        case PackagingType.TYPE_ECLIPSE_FEATURE:
            updateVersionQualiferInFeatureXml(newQualifier);
            break;
        }
        updatePomVersion();
    }

    private void updatePomVersion() throws MojoExecutionException, MojoFailureException {
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
            String newVersion = createNewPomVersion(oldVersion, getNewMavenVersionQualifier());
            pom.setVersion(newVersion);
            MutablePomFile.write(pom, pomFile);
            logVersionChanged(oldVersion, newVersion, pomFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private void updateVersionQualiferInManifest(String newQualifier) throws MojoExecutionException {
        File manifestFile = new File(project.getBasedir(), "META-INF/MANIFEST.MF");
        try {
            MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
            String oldVersion = manifest.getVersion();
            if (!matchesOsgiVersionPattern(oldVersion, manifestFile)) {
                return;
            }
            String newVersion = createNewOsgiVersion(oldVersion, newQualifier);
            validateOsgiVersion(newVersion);
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
            String newVersion = createNewOsgiVersion(oldVersion, qualifier);
            validateOsgiVersion(newVersion);
            feature.setVersion(newVersion);
            Feature.write(feature, featureXmlFile);
            logVersionChanged(oldVersion, newVersion, featureXmlFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean matchesOsgiVersionPattern(String version, File source) {
        Pattern osgiVersionPattern = getOldOsgiVersionPattern();
        boolean isValid = osgiVersionPattern.matcher(version).matches();
        if (!isValid) {
            getLog().warn(String.format("Version '%s' in '%s' does not match regexp pattern '"
                    + osgiVersionPattern.pattern() + "' - skipping.", version, source));
        }
        return isValid;
    }

    private boolean matchesMavenVersionPattern(String version, File pomFile) {
        Pattern mavenVersionPattern = getOldMavenVersionPattern();
        boolean isValid = mavenVersionPattern.matcher(version).matches();
        if (!isValid) {
            getLog().warn(String.format("Version '%s' in '%s' does not match regexp pattern '"
                    + mavenVersionPattern.pattern() + "' - skipping.", version, pomFile));
        }
        return isValid;
    }

    private void logVersionChanged(String oldVersion, String newVersion, File file) {
        getLog().info(
                String.format("Changed version: '%s' -> '%s' in %s", oldVersion, newVersion, file.getAbsolutePath()));
    }

    private void validateOsgiVersion(String version) throws MojoExecutionException {
        try {
            Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
