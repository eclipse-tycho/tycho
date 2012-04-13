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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.osgi.framework.Version;

/**
 * This mojo generates build qualifier according to the rules outlined in
 * http://help.eclipse.org/ganymede/topic/org.eclipse.pde.doc.user/tasks/pde_version_qualifiers.htm
 * <ol>
 * <li>explicit -DforceContextQualifier command line parameter</li>
 * <li>forceContextQualifier from ${project.baseDir}/build.properties</li>
 * <li>a time stamp in the form YYYYMMDDHHMM (ie 200605121600)</li>
 * </ol>
 * 
 * <p>
 * The generated qualifier is assigned to <code>buildQualifier</code> project property. Unqualified
 * project version is assigned to <code>unqualifiedVersion</code> project property. Unqualified
 * version is calculated based on <code>${project.version}</code> and can be used for any Tycho
 * project (eclipse-update-site, eclipse-application, etc) and regular maven project. Different
 * projects can use different formats to expand the timestamp, however (highly not recommended but
 * possible).
 * 
 * <p>
 * For "aggregate" project packaging types, like eclipse-feature, build timestamp is calculated as
 * the latest timestamp of the project itself and timestamps of bundles and features directly
 * included in the project. This is meant to work with custom timestamp providers (see below) and
 * generate build qualifier based on build contents, i.e. the source code, and not the time the
 * build was started.
 * 
 * <p>
 * Starting with version 0.16, it is now possible to use custom build timestamp generation logic.
 * The primary usecase is to generate build version qualifier based on the timestamp of the last
 * project commit. Here is example pom.xml snippet that enables custom timestamp generation logic
 * 
 * <pre>
 *      ...
 *      &lt;plugin>
 *         &lt;groupId>org.eclipse.tycho&lt;/groupId>
 *         &lt;artifactId>tycho-packaging-plugin&lt;/artifactId>
 *         &lt;version>${tycho-version}&lt;/version>
 *         &lt;dependencies>
 *           &lt;dependency>
 *             &lt;groupId>timestamp-provider-groupid&lt;/groupId>
 *             &lt;artifactId>timestamp-provider-artifactid&lt;/artifactId>
 *             &lt;version>timestamp-provider-version&lt;/version>
 *           &lt;/dependency>
 *         &lt;/dependencies>
 *         &lt;configuration>
 *           &lt;timestampProvider>custom&lt;/timestampProvider>
 *         &lt;/configuration>
 *      &lt;/plugin>
 *      ...
 * 
 * </pre>
 * 
 * @goal build-qualifier
 * @phase validate
 */
public class BuildQualifierMojo extends AbstractVersionMojo {

    public static final String BUILD_QUALIFIER_PROPERTY = "buildQualifier";

    public static final String UNQUALIFIED_VERSION_PROPERTY = "unqualifiedVersion";

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * Specify a date format as specified by java.text.SimpleDateFormat. Timezone used is UTC.
     * 
     * @parameter default-value="yyyyMMddHHmm"
     */
    private SimpleDateFormat format;

    /**
     * @parameter default-value="${project.basedir}"
     */
    private File baseDir;

    /**
     * @parameter expression="${forceContextQualifier}"
     */
    private String forceContextQualifier;

    /**
     * Role hint of a custom build timestamp provider.
     * 
     * @parameter
     */
    private String timestampProvider;

    /**
     * @parameter expression="${mojoExecution}"
     */
    private MojoExecution execution;

    /**
     * @component
     */
    private BuildPropertiesParser buildPropertiesParser;

    /**
     * @component role="org.eclipse.tycho.buildversion.BuildTimestampProvider"
     */
    private Map<String, BuildTimestampProvider> timestampProviders;

    // setter is needed to make sure we always use UTC
    public void setFormat(String formatString) {
        format = new SimpleDateFormat(formatString);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        String osgiVersionStr = getOSGiVersion();
        if (osgiVersionStr != null) {
            try {
                Version osgiVersion = Version.parseVersion(osgiVersionStr);

                if (!VersioningHelper.QUALIFIER.equals(osgiVersion.getQualifier())) {
                    // fully expended or absent qualified. nothing to expand
                    project.getProperties().put(BUILD_QUALIFIER_PROPERTY, osgiVersion.getQualifier());
                    project.getProperties().put(UNQUALIFIED_VERSION_PROPERTY,
                            osgiVersion.getMajor() + "." + osgiVersion.getMinor() + "." + osgiVersion.getMicro());

                    return;
                }
            } catch (IllegalArgumentException e) {
                throw new MojoFailureException("Not a valid OSGi version " + osgiVersionStr + " for project " + project);
            }
        }

        String qualifier = forceContextQualifier;

        if (qualifier == null) {
            qualifier = buildPropertiesParser.parse(baseDir).getForceContextQualifier();
        }

        if (qualifier == null) {
            Date timestamp = getBuildTimestamp();

            // TODO make stable qualifier logic optional

            final Date[] latestTimestamp = new Date[] { timestamp };

            TychoProject projectType = projectTypes.get(project.getPackaging());
            if (projectType == null) {
                throw new IllegalStateException("Unknown or unsupported packaging type " + packaging);
            }

            projectType.getDependencyWalker(project).walk(new ArtifactDependencyVisitor() {
                @Override
                public boolean visitFeature(FeatureDescription feature) {
                    if (feature.getFeatureRef() == null) {
                        // 'this' feature
                        return true; // visit immediately included features
                    }
                    visitArtifact(feature);
                    return false; // do not visit indirectly included features/bundles
                }

                @Override
                public void visitPlugin(PluginDescription plugin) {
                    if (plugin.getPluginRef() == null) {
                        // 'this' bundle
                        return;
                    }
                    visitArtifact(plugin);
                }

                private void visitArtifact(ArtifactDescriptor artifact) {
                    ReactorProject otherProject = artifact.getMavenProject();
                    String otherVersion = (otherProject != null) ? otherVersion = otherProject.getExpandedVersion()
                            : artifact.getKey().getVersion();
                    Version v = Version.parseVersion(otherVersion);
                    String otherQualifier = v.getQualifier();
                    if (otherQualifier != null) {
                        Date timestamp = parseQualifier(otherQualifier);
                        if (timestamp != null) {
                            if (latestTimestamp[0].compareTo(timestamp) < 0) {
                                latestTimestamp[0] = timestamp;
                            }
                        } else {
                            getLog().debug("Could not parse qualifier timestamp " + otherQualifier);
                        }
                    }
                }

                private Date parseQualifier(String qualifier) {
                    return parseQualifier(qualifier, format);
                }

                private Date parseQualifier(String qualifier, SimpleDateFormat format) {
                    ParsePosition pos = new ParsePosition(0);
                    Date timestamp = format.parse(qualifier, pos);
                    if (timestamp != null && pos.getIndex() == qualifier.length()) {
                        return timestamp;
                    }
                    return null;
                }
            });

            qualifier = getQualifier(latestTimestamp[0]);
        }

        project.getProperties().put(BUILD_QUALIFIER_PROPERTY, qualifier);
        project.getProperties().put(UNQUALIFIED_VERSION_PROPERTY, getUnqualifiedVersion());
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

    private Date getBuildTimestamp() throws MojoExecutionException {
        String hint = timestampProvider != null ? timestampProvider : DefaultBuildTimestampProvider.ROLE_HINT;
        BuildTimestampProvider provider = timestampProviders.get(hint);
        if (provider == null) {
            throw new MojoExecutionException("Unable to lookup BuildTimestampProvider with hint='" + hint + "'");
        }
        return provider.getTimestamp(session, project, execution);
    }

}
