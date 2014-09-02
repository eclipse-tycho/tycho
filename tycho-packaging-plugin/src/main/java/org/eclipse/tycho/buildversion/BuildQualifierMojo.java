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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.osgi.framework.Version;

/**
 * <p>
 * This mojo generates the build qualifier according to the <a href=
 * "http://help.eclipse.org/kepler/topic/org.eclipse.pde.doc.user/tasks/pde_version_qualifiers.htm"
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
 * expand the timestamp (not recommended).
 * </p>
 * <p>
 * The timestamp generation logic is extensible. The primary use case is to generate build version
 * qualifier based on the timestamp of the last project commit. Here is example pom.xml snippet that
 * enables custom timestamp generation logic
 * 
 * <pre>
 * ...
 * &lt;plugin>
 *    &lt;groupId>org.eclipse.tycho&lt;/groupId>
 *    &lt;artifactId>tycho-packaging-plugin&lt;/artifactId>
 *    &lt;version>${tycho-version}&lt;/version>
 *    &lt;dependencies>
 *      &lt;dependency>
 *        &lt;groupId>timestamp-provider-groupid&lt;/groupId>
 *        &lt;artifactId>timestamp-provider-artifactid&lt;/artifactId>
 *        &lt;version>timestamp-provider-version&lt;/version>
 *      &lt;/dependency>
 *    &lt;/dependencies>
 *    &lt;configuration>
 *      &lt;timestampProvider>custom&lt;/timestampProvider>
 *    &lt;/configuration>
 * &lt;/plugin>
 * ...
 * 
 * </pre>
 * 
 */
@Mojo(name = "build-qualifier", defaultPhase = LifecyclePhase.VALIDATE)
public class BuildQualifierMojo extends AbstractVersionMojo {

    public static final String BUILD_QUALIFIER_PROPERTY = "buildQualifier";

    public static final String UNQUALIFIED_VERSION_PROPERTY = "unqualifiedVersion";

    @Parameter(property = "session", readonly = true)
    protected MavenSession session;

    /**
     * <p>
     * Specify a date format as specified by java.text.SimpleDateFormat. Timezone used is UTC.
     * </p>
     */
    @Parameter(defaultValue = "yyyyMMddHHmm")
    protected SimpleDateFormat format;

    /**
     * @deprecated This parameter is deprecated and may be removed in future versions of Tycho.
     */
    // TODO this should not be configurable
    @Parameter(property = "project.basedir")
    protected File baseDir;

    @Parameter(property = "forceContextQualifier")
    protected String forceContextQualifier;

    /**
     * <p>
     * Role hint of a custom build timestamp provider.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter
    protected String timestampProvider;

    @Parameter(property = "mojoExecution", readonly = true)
    protected MojoExecution execution;

    @Component
    protected BuildPropertiesParser buildPropertiesParser;

    @Component(role = BuildTimestampProvider.class)
    protected Map<String, BuildTimestampProvider> timestampProviders;

    // setter is needed to make sure we always use UTC
    public void setFormat(String formatString) {
        format = new SimpleDateFormat(formatString);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        TychoProjectVersion projectVersion = calculateQualifiedVersion();
        project.getProperties().put(BUILD_QUALIFIER_PROPERTY, projectVersion.qualifier);
        project.getProperties().put(UNQUALIFIED_VERSION_PROPERTY, projectVersion.unqualifiedVersion);

        getLog().info("The project's OSGi version is " + projectVersion.getOSGiVersion());
    }

    private TychoProjectVersion calculateQualifiedVersion() throws MojoFailureException, MojoExecutionException {
        Version osgiVersion = getParsedOSGiVersion();
        if (osgiVersion != null) {

            if (!VersioningHelper.QUALIFIER.equals(osgiVersion.getQualifier())) {
                // fully expended or absent qualified. nothing to expand
                String unqualifiedVersion = osgiVersion.getMajor() + "." + osgiVersion.getMinor() + "."
                        + osgiVersion.getMicro();
                return new TychoProjectVersion(unqualifiedVersion, osgiVersion.getQualifier());
            }
        }

        String qualifier = forceContextQualifier;

        if (qualifier == null) {
            qualifier = buildPropertiesParser.parse(baseDir).getForceContextQualifier();
        }

        if (qualifier == null) {
            Date timestamp = getBuildTimestamp();
            qualifier = getQualifier(timestamp);
        }

        return new TychoProjectVersion(getUnqualifiedVersion(), qualifier);
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

    String getQualifier(Date timestamp) {
        return format.format(timestamp);
    }

    private String getUnqualifiedVersion() {
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
            if (qualifier.length() == 0) {
                return unqualifiedVersion;
            } else {
                return unqualifiedVersion + '.' + qualifier;
            }
        }
    }
}
