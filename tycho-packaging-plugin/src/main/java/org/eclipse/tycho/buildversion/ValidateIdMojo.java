package org.eclipse.tycho.buildversion;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.ArtifactKey;

/**
 * Validates that project Maven and OSGi ids match.
 * 
 * @goal validate-id
 * @phase validate
 */

public class ValidateIdMojo extends AbstractVersionMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.getArtifactId().equals(getOSGiId())) {
            failBuildDueToIdMismatch();
        }
    }

    private void failBuildDueToIdMismatch() throws MojoExecutionException {
        if (ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging())) {
            throw new MojoExecutionException(mismatchMessageFor("feature ID"));
        } else {
            throw new MojoExecutionException(mismatchMessageFor("bundle symbolic name"));
        }
    }

    private String mismatchMessageFor(String eclipseIdKey) {
        return String.format("The Maven artifactId (currently: \"%1s\") must be the same as the " + eclipseIdKey
                + " (currently: \"%2s\")", project.getArtifactId(), getOSGiId());
    }
}
