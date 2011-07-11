/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.ArtifactKey;

/**
 * Validates project Maven and OSGi versions. For SNAPSHOT versions, OSGi version qualifier must be
 * ".qualifier" and unqualified Maven and OSGi versions must be equal. For RELEASE versions, OSGi
 * and Maven versions must be equal.
 * 
 * @goal validate-version
 * @phase validate
 */
public class ValidateVersionMojo extends AbstractVersionMojo {
    /**
     * If <code>true</code> (the default) will fail the build if Maven and OSGi project versions do
     * not match. If <code>false</code> will issue a warning but will not fail the build if Maven
     * and OSGi project versions do not match.
     * 
     * @parameter default-value="true"
     */
    private boolean strictVersions = true;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String mavenVersion = project.getVersion();
        String osgiVersion = getOSGiVersion();

        if (osgiVersion == null) {
            return;
        }

        if (project.getArtifact().isSnapshot()) {
            validateSnapshotVersion(mavenVersion, osgiVersion);
        } else {
            validateReleaseVersion(mavenVersion, osgiVersion);
        }
    }

    public void validateReleaseVersion(String mavenVersion, String osgiVersion) throws MojoExecutionException {
        if (!mavenVersion.equals(osgiVersion)) {
            fail("OSGi version " + osgiVersion + " in " + getOSGiMetadataFileName() + " does not match Maven version "
                    + mavenVersion + " in pom.xml");
        }
    }

    private String getOSGiMetadataFileName() {
        String packaging = project.getPackaging();
        // TODO this does not belong here, packaging type should know about its metadata file
        if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
            return "META-INF/MANIFEST.MF";
        } else if (ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
            return "feature.xml";
        } else if (ArtifactKey.TYPE_ECLIPSE_APPLICATION.equals(packaging)) {
            return project.getArtifactId() + ".product";
        } else if (ArtifactKey.TYPE_ECLIPSE_REPOSITORY.equals(packaging)) {
            return project.getArtifactId();
        }
        return "<unknown packaging=" + packaging + ">";
    }

    public void validateSnapshotVersion(String mavenVersion, String osgiVersion) throws MojoExecutionException {
        if (!osgiVersion.endsWith(VersioningHelper.QUALIFIER)) {
            fail("OSGi version " + osgiVersion + " must have .qualifier qualifier for SNAPSHOT builds");
        } else {
            String unqualifiedMavenVersion = mavenVersion.substring(0, mavenVersion.length() - "-SNAPSHOT".length());
            String unqualifiedOSGiVersion = osgiVersion.substring(0,
                    osgiVersion.length() - VersioningHelper.QUALIFIER.length() - 1);
            if (!unqualifiedMavenVersion.equals(unqualifiedOSGiVersion)) {
                fail("Unqualified OSGi version " + osgiVersion + " must match unqualified Maven version "
                        + mavenVersion + " for SNAPSHOT builds");
            }
        }
    }

    private void fail(String message) throws MojoExecutionException {
        if (strictVersions) {
            throw new MojoExecutionException(message);
        } else {
            getLog().warn(message);
        }
    }
}
