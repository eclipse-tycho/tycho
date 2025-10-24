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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.ManifestHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.osgi.Constants;

/**
 * Validates that project Maven and OSGi ids match.
 */
@Mojo(name = "validate-id", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ValidateIdMojo extends AbstractVersionMojo {
    /**
     * Whether to skip the project's artifact ID validation against the OSGi ID.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

	@Inject
	ManifestHelper manifestHelper;

	@Inject
	BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
		File file = getOSGiMetadataFile();
		buildContext.removeMessages(file);
        if (!skip && !project.getArtifactId().equals(getOSGiId())) {
			String message;
			int lineNumber;
			int column;
			if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging())) {
				message = mismatchMessageFor("feature ID");
				// TODO maybe have an XML helper to get the line/col of an XMLElement +
				// attribute?
				lineNumber = 0;
				column = 0;
			} else if (PackagingType.TYPE_P2_IU.equals(project.getPackaging())) {
				message = mismatchMessageFor("iu ID");
				lineNumber = 0;
				column = 0;
			} else {
				message = mismatchMessageFor("bundle symbolic name");
				lineNumber = manifestHelper.getLineNumber(file, Constants.BUNDLE_SYMBOLICNAME);
				column = 0;
			}
			buildContext.addMessage(file, lineNumber, column, message, BuildContext.SEVERITY_ERROR, null);
			throw new MojoExecutionException(message);
        }
    }

    private String mismatchMessageFor(String eclipseIdKey) {
        return String.format("The Maven artifactId (currently: \"%1s\") must be the same as the " + eclipseIdKey
                + " (currently: \"%2s\")", project.getArtifactId(), getOSGiId());
    }
}
