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
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.osgi.framework.Version;

/**
 * This mojo generates build qualifier according to the rules outlined in
 * http://help.eclipse.org/ganymede/topic/org.eclipse.pde.doc.user/tasks/pde_version_qualifiers.htm
 * <ol>
 * <li>explicit -DforceContextQualifier command line parameter</li>
 * <li>forceContextQualifier from ${project.baseDir}/build.properties</li>
 * <li>the tag that was used to fetch the bundle (only when using map file)</li>
 * <li>a time stamp in the form YYYYMMDDHHMM (ie 200605121600)</li>
 * </ol>
 * The generated qualifier is assigned to <code>buildQualifier</code> project property. Unqualified
 * project version is assigned to <code>unqualifiedVersion</code> project property. Unqualified
 * version is calculated based on <code>${project.version}</code> and can be used for any Tycho
 * project (eclipse-update-site, eclipse-application, etc) and regular maven project. Implementation
 * guarantees that the same timestamp is used for all projects in reactor build. Different projects
 * can use different formats to expand the timestamp, however (highly not recommended but possible).
 * 
 * @goal build-qualifier
 * @phase validate
 */
public class BuildQualifierMojo extends AbstractVersionMojo {

    public static final String BUILD_QUALIFIER_PROPERTY = "buildQualifier";

    public static final String UNQUALIFIED_VERSION_PROPERTY = "unqualifiedVersion";

    private static final String REACTOR_BUILD_TIMESTAMP_PROPERTY = "reactorBuildTimestampProperty";

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * Specify a message format as specified by java.text.SimpleDateFormat. Timezone used is UTC.
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
     * @component
     */
    private BuildPropertiesParser buildPropertiesParser;

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
            Date timestamp = getSessionTimestamp();
            qualifier = getQualifier(timestamp);
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

    private Date getSessionTimestamp() {
        Date timestamp;
        String value = session.getUserProperties().getProperty(REACTOR_BUILD_TIMESTAMP_PROPERTY);
        if (value != null) {
            timestamp = new Date(Long.parseLong(value));
        } else {
            timestamp = new Date();
            session.getUserProperties().setProperty(REACTOR_BUILD_TIMESTAMP_PROPERTY,
                    Long.toString(timestamp.getTime()));
        }
        return timestamp;
    }

}
