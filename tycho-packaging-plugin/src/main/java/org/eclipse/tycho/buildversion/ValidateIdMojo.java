/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.tycho.PackagingType;

/**
 * Validates that project Maven and OSGi ids match.
 */
@Mojo(name = "validate-id", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateIdMojo extends AbstractVersionMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.getArtifactId().equals(getOSGiId())) {
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
