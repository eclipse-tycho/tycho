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

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public abstract class AbstractJUnitProvider implements TestFrameworkProvider {

    private Version version;

    public AbstractJUnitProvider() {
    }

    @Override
    public String getType() {
        return "junit";
    }

    protected Version loadVersionFromClasspath(Version defaultVersion) {
        if (version == null) {
            try {
                String name = "META-INF/org/eclipse/tycho/surefire/provider/" + getClass().getSimpleName()
                        + "/version.properties";
                URL versionFile = getClass().getClassLoader().getResource(name);
                if (versionFile == null) {
                    version = defaultVersion;
                } else {
                    try (InputStream openStream = versionFile.openStream()) {
                        Properties properties = new Properties();
                        properties.load(openStream);
                        version = Version.parseVersion(properties.getProperty("junitVersion"));
                    }
                }
            } catch (Exception e) {
                version = defaultVersion;
            }
        }
        return Objects.requireNonNullElse(version, defaultVersion);

    }

    public abstract boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties);

    protected static boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Set<String> junitBundleNames, VersionRange range) {
        if (containsJunitInClasspath(testBundleClassPath, junitBundleNames, range)) {
            return true;
        }
        if (project != null) {
            return containsJunitInDependencies(project, junitBundleNames, range);
        }
        return false;
    }

    protected static boolean containsJunitInDependencies(MavenProject project, Set<String> junitBundleNames,
            VersionRange range) {
        for (Artifact artifact : project.getArtifacts()) {
            if (Artifact.SCOPE_TEST.equals(artifact.getScope())
                    && junitBundleNames.contains(artifact.getArtifactId())) {
                try {
                    Version version = Version.parseVersion(artifact.getVersion());
                    if (range.includes(version)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    //just continue, seems not a valid (osgi) version then...
                }
            }
        }
        return false;
    }

    protected static boolean containsJunitInClasspath(List<ClasspathEntry> testBundleClassPath,
            Set<String> junitBundleNames, VersionRange range) {
        for (ClasspathEntry classpathEntry : testBundleClassPath) {
            ArtifactKey artifactKey = classpathEntry.getArtifactKey();
            if (junitBundleNames.contains(artifactKey.getId())) {
                Version version = Version.parseVersion(artifactKey.getVersion());
                if (range.includes(version)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Properties getProviderSpecificProperties() {
        return new Properties();
    }

}
