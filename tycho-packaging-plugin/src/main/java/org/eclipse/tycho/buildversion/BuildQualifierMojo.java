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
 *    Christoph Läubrich - Issue #611 - Support setting CI-Friendly-Versions in tycho-build-extension
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import static org.eclipse.tycho.TychoProperties.BUILD_QUALIFIER;
import static org.eclipse.tycho.TychoProperties.QUALIFIED_VERSION;
import static org.eclipse.tycho.TychoProperties.UNQUALIFIED_VERSION;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.build.BuildTimestampProvider;
import org.eclipse.tycho.core.VersioningHelper;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.osgi.framework.Version;

/**
 * <p>
 * This mojo generates the build qualifier according to the <a href=
 * "https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/tasks/pde_version_qualifiers.htm"
 * >rules described in the PDE documentation</a>:
 * <ol>
 * <li>Explicit -DforceContextQualifier command line parameter</li>
 * <li>forceContextQualifier from ${project.baseDir}/build.properties</li>
 * <li>A time stamp in the form YYYYMMDDHHMM (e.g. 200605121600)</li>
 * </ol>
 * </p>
 * <p>
 * The generated qualifier is assigned to <code>buildQualifier</code> project property. The
 * unqualified project version is assigned to <code>unqualifiedVersion</code> project property. The
 * unqualified version is calculated based on <code>${project.version}</code> and can be used for
 * any Tycho project and regular Maven project. Different projects can use different formats to
 * expand the timestamp (not recommended). The concatenation of <code>${unqualifiedVersion}</code>
 * and <code>${buildQualifier}</code>, if not empty, is assigned to the project property
 * <code>qualifiedVersion</code>.
 * </p>
 * <p>
 * The timestamp generation logic is extensible. The primary use case is to generate build version
 * qualifier based on the timestamp of the last project commit. Here is example pom.xml snippet that
 * enables custom timestamp generation logic
 * 
 * <pre>
 * ...
 * &lt;plugin&gt;
 *    &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
 *    &lt;artifactId&gt;tycho-packaging-plugin&lt;/artifactId&gt;
 *    &lt;version&gt;${tycho-version}&lt;/version&gt;
 *    &lt;dependencies&gt;
 *      &lt;dependency&gt;
 *        &lt;groupId&gt;timestamp-provider-groupid&lt;/groupId&gt;
 *        &lt;artifactId&gt;timestamp-provider-artifactid&lt;/artifactId&gt;
 *        &lt;version&gt;timestamp-provider-version&lt;/version&gt;
 *      &lt;/dependency&gt;
 *    &lt;/dependencies&gt;
 *    &lt;configuration&gt;
 *      &lt;timestampProvider&gt;custom&lt;/timestampProvider&gt;
 *    &lt;/configuration&gt;
 * &lt;/plugin&gt;
 * ...
 * 
 * </pre>
 * 
 */
@Mojo(name = "build-qualifier", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BuildQualifierMojo extends AbstractVersionMojo {

	static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

	static final String PARAMETER_FORMAT = "format";

	static final String DEFAULT_DATE_FORMAT = "yyyyMMddHHmm";

	@Parameter(property = "session", readonly = true)
    protected MavenSession session;

    /**
     * <p>
     * Specify a date format as specified by java.text.SimpleDateFormat. Timezone used is UTC.
     * </p>
     */
	@Parameter(name = PARAMETER_FORMAT, defaultValue = DEFAULT_DATE_FORMAT, property = "tycho.buildqualifier.format")
    protected SimpleDateFormat format;

    @Parameter(property = "forceContextQualifier")
    protected String forceContextQualifier;

    /**
     * <p>
     * Role hint of a custom build timestamp provider.
     * </p>
     * 
     * @since 0.16.0
     */
	@Parameter(property = "tycho.buildqualifier.provider")
    protected String timestampProvider;

    @Parameter(property = "mojoExecution", readonly = true)
    protected MojoExecution execution;

	@Inject
	protected Map<String, BuildTimestampProvider> timestampProviders;

	@Inject
	private BuildPropertiesParser buildPropertiesParser;

	/**
	 * This is only a dummy parameter used to prevent maven from complaining about
	 * "unknown" parameters when using the jgit extension
	 */
	@Parameter(alias = "jgit.dirtyWorkingTree")
	private String dummy1;

	/**
	 * This is only a dummy parameter used to prevent maven from complaining about
	 * "unknown" parameters when using the jgit extension
	 */
	@Parameter(alias = "jgit.ignore")
	private String dummy2;

    // setter is needed to make sure we always use UTC
    public void setFormat(String formatString) {
        format = new SimpleDateFormat(formatString);
        format.setTimeZone(TIME_ZONE);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
		Date timestamp = getBuildTimestamp();
		TychoProjectVersion projectVersion = calculateQualifiedVersion(timestamp);
		project.getProperties().put(BUILD_QUALIFIER, projectVersion.qualifier);
		project.getProperties().put(UNQUALIFIED_VERSION, projectVersion.unqualifiedVersion);
		project.getProperties().put(QUALIFIED_VERSION, projectVersion.getOSGiVersion());
		getLog().info("The project's OSGi version is " + projectVersion.getOSGiVersion());
		DefaultReactorProject.adapt(project).setContextValue(TychoConstants.BUILD_TIMESTAMP, projectVersion);
    }

	private TychoProjectVersion calculateQualifiedVersion(Date timestamp)
			throws MojoFailureException, MojoExecutionException {

        Version osgiVersion = getParsedOSGiVersion();
        if (osgiVersion != null) {

            if (!VersioningHelper.QUALIFIER.equals(osgiVersion.getQualifier())) {
                // fully expended or absent qualified. nothing to expand
                String unqualifiedVersion = osgiVersion.getMajor() + "." + osgiVersion.getMinor() + "."
                        + osgiVersion.getMicro();
                return new TychoProjectVersion(unqualifiedVersion, osgiVersion.getQualifier());
            }
        }

		String forceContextQualifier = getForceContextQualifier();
		String qualifier = getDesiredQualifier(forceContextQualifier, timestamp);

		validateQualifier(forceContextQualifier, qualifier);

		String pomOSGiVersion = getUnqualifiedVersion();
		String suffix = "." + qualifier;
		if (pomOSGiVersion.endsWith(suffix)) {
			return new TychoProjectVersion(pomOSGiVersion.substring(0, pomOSGiVersion.length() - suffix.length()),
					qualifier);
		}
		return new TychoProjectVersion(pomOSGiVersion, qualifier);
    }

    protected String getDesiredQualifier(String forceContextQualifier, Date timestamp) throws MojoExecutionException {
        String qualifier = forceContextQualifier;
        if (TychoConstants.QUALIFIER_NONE.equals(qualifier)) {
            qualifier = "";
        }

        if (qualifier == null) {
            qualifier = getQualifier(timestamp);
        }
        return qualifier;
    }

    private String getForceContextQualifier() {
        String qualifier = forceContextQualifier;

        if (qualifier == null) {
            qualifier = buildPropertiesParser.parse(DefaultReactorProject.adapt(project)).getForceContextQualifier();
        }
        return qualifier;
    }

    private Version getParsedOSGiVersion() throws MojoFailureException {
        String osgiVersionString = getOSGiVersion();
        if (osgiVersionString == null) {
            return null;
        }

        try {
            return Version.parseVersion(osgiVersionString);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException("Not a valid OSGi version " + osgiVersionString + " for project " + project);
        }
    }

    void validateQualifier(String forceContextQualifier, String qualifier) throws MojoFailureException {
        if (TychoConstants.QUALIFIER_NONE.equals(forceContextQualifier)) {
            return;
        }
        // parse a valid version with the given qualifier to check if the qualifier is valid
        try {
            Version.parseVersion("1.0.0." + qualifier);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException("Invalid build qualifier '" + qualifier
                    + "', it does not match the OSGi qualifier constraint ([0..9]|[a..zA..Z]|'_'|'-')");
        }
    }

    String getQualifier(Date timestamp) {
        return format.format(timestamp);
    }

	private String getUnqualifiedVersion() {
		// First try to handle this as an already valid OSGi version
		try {
			Version version = Version.parseVersion(project.getVersion());
			return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
		} catch (RuntimeException e) {
		}
		// then try the "selected version"
		try {
			ArtifactVersion version = project.getArtifact().getSelectedVersion();
			int majorVersion = version.getMajorVersion();
			int minorVersion = version.getMinorVersion();
			int incrementalVersion = version.getIncrementalVersion();
			if (majorVersion > 0 || minorVersion > 0 || incrementalVersion > 0) {
				return majorVersion + "." + minorVersion + "." + incrementalVersion;
			}
		} catch (OverConstrainedVersionException e) {
		}
		// last resort ...
		String version = project.getArtifact().getVersion();
		if (version.endsWith("-" + Artifact.SNAPSHOT_VERSION)) {
			version = version.substring(0, version.length() - Artifact.SNAPSHOT_VERSION.length() - 1);
		}
		return version;
	}

    protected Date getBuildTimestamp() throws MojoExecutionException {
        String hint = timestampProvider != null ? timestampProvider : DefaultBuildTimestampProvider.ROLE_HINT;
        BuildTimestampProvider provider = timestampProviders.get(hint);
        if (provider == null) {
            throw new MojoExecutionException("Unable to lookup BuildTimestampProvider with hint='" + hint + "'");
        }
        return provider.getTimestamp(session, project, execution);
    }

    // TODO 382482 use this class throughout Tycho? 
    static class TychoProjectVersion {

        // TODO also store Maven version? make constraints enforced by ValidateVersionMojo invariants of this class?
        private String unqualifiedVersion;
        private String qualifier;

        TychoProjectVersion(String unqualifiedVersion, String qualifier) {
            this.unqualifiedVersion = unqualifiedVersion;
            this.qualifier = qualifier == null ? "" : qualifier;
        }

        public String getOSGiVersion() {
            if (qualifier.isEmpty()) {
                return unqualifiedVersion;
            } else {
                return unqualifiedVersion + '.' + qualifier;
            }
        }
    }
}
