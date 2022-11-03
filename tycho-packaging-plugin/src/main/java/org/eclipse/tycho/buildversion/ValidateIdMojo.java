/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - add support for IU type (428310)
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.PackagingType;

/**
 * Validates that project Maven and OSGi ids match.
 */
@Mojo(name = "validate-id", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ValidateIdMojo extends AbstractVersionMojo {
    /**
     * Whether to skip the project's artifact ID validation against the OSGi ID.
     */
    @Parameter
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skip && !project.getArtifactId().equals(getOSGiId())) {
            failBuildDueToIdMismatch();
        }
    }

    private void failBuildDueToIdMismatch() throws MojoExecutionException {
        if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging())) {
            throw new MojoExecutionException(mismatchMessageFor("feature ID"));
        }
        if (PackagingType.TYPE_P2_IU.equals(project.getPackaging())) {
            throw new MojoExecutionException(mismatchMessageFor("iu ID"));
        }
        throw new MojoExecutionException(mismatchMessageFor("bundle symbolic name"));
    }

    private String mismatchMessageFor(String eclipseIdKey) {
        return String.format("The Maven artifactId (currently: \"%1s\") must be the same as the " + eclipseIdKey
                + " (currently: \"%2s\")", project.getArtifactId(), getOSGiId());
    }
}
