/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
 *    Christoph Läubrich - #611 Support setting CI-Friendly-Versions in tycho-build-extension
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoProperties;
import org.eclipse.tycho.core.ManifestHelper;
import org.eclipse.tycho.core.VersioningHelper;
import org.osgi.framework.Constants;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Validates project Maven and OSGi versions. For SNAPSHOT versions, OSGi
 * version qualifier must be ".qualifier" and unqualified Maven and OSGi
 * versions must be equal. For RELEASE versions, OSGi and Maven versions must be
 * equal.
 */
@Mojo(name = "validate-version", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ValidateVersionMojo extends AbstractVersionMojo {
	/**
	 * If <code>true</code> (the default) will fail the build if Maven and OSGi
	 * project versions do not match. If <code>false</code> will issue a warning but
	 * will not fail the build if Maven and OSGi project versions do not match.
	 */
	@Parameter(defaultValue = "true", property = "tycho.strictVersions")
	private boolean strictVersions = true;

	@Inject
	ManifestHelper manifestHelper;

	@Inject
	BuildContext buildContext;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		buildContext.removeMessages(getOSGiMetadataFile());
		String mavenVersion = project.getVersion();
		String osgiVersion = getOSGiVersion();

		if (osgiVersion == null) {
			return;
		}
		Object qualifiedVersion = project.getProperties().get(TychoProperties.QUALIFIED_VERSION);
		if (mavenVersion.equals(qualifiedVersion)) {
			return;
		}

		if (project.getArtifact().isSnapshot() || osgiVersion.endsWith(VersioningHelper.QUALIFIER)) {
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

	public void validateSnapshotVersion(String mavenVersion, String osgiVersion) throws MojoExecutionException {
		if (!mavenVersion.endsWith(Artifact.SNAPSHOT_VERSION)) {
			fail("Maven version " + mavenVersion + " must have -SNAPSHOT qualifier for SNAPSHOT builds");
		}
		if (!osgiVersion.endsWith(VersioningHelper.QUALIFIER)) {
			fail("OSGi version " + osgiVersion + " must have .qualifier qualifier for SNAPSHOT builds");
		} else {
			String unqualifiedMavenVersion = mavenVersion;
			if (mavenVersion.endsWith(Artifact.SNAPSHOT_VERSION)) {
				unqualifiedMavenVersion = mavenVersion.substring(0,
						mavenVersion.length() - Artifact.SNAPSHOT_VERSION.length() - 1);
			}
			String unqualifiedOSGiVersion = osgiVersion.substring(0,
					osgiVersion.length() - VersioningHelper.QUALIFIER.length() - 1);
			if (!unqualifiedMavenVersion.equals(unqualifiedOSGiVersion)) {
				fail("Unqualified OSGi version " + osgiVersion + " must match unqualified Maven version " + mavenVersion
						+ " for SNAPSHOT builds");
			}
		}
	}

	private void fail(String message) throws MojoExecutionException {
		File metaDataFile = getOSGiMetadataFile();
		int lineNumber;
		int column;
		if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
				|| PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
			lineNumber = manifestHelper.getLineNumber(metaDataFile, Constants.BUNDLE_VERSION);
			column = 0;
		} else {
			lineNumber = 0;
			column = 0;
		}
		int serv = strictVersions ? BuildContext.SEVERITY_ERROR : BuildContext.SEVERITY_WARNING;
		if (buildContext != null) {
			buildContext.addMessage(metaDataFile, lineNumber, column, message, serv, null);
		}
		if (strictVersions) {
			throw new MojoExecutionException(message);
		} else {
			getLog().warn(message);
		}
	}
}
