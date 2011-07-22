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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
     */
    private MavenSession session;

    /**
     * Specify a message format as specified by java.text.SimpleDateFormat. Timezone used is UTC.
     * Default value is "yyyyMMddHHmm".
     * 
     * @parameter
     */
    private SimpleDateFormat format = createUTCDateFormat("yyyyMMddHHmm");

    /**
     * @parameter default-value="${project.basedir}/build.properties"
     */
    private File buildPropertiesFile;

    /**
     * @parameter expression="${forceContextQualifier}"
     */
    private String forceContextQualifier;

    public void setFormat(String format) {
        this.format = createUTCDateFormat(format);
    }

    private SimpleDateFormat createUTCDateFormat(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
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
            qualifier = getBuildProperties().getProperty("forceContextQualifier");
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

    // TODO move to a helper, we must have ~100 implementations of this logic
    private Properties getBuildProperties() {
        Properties props = new Properties();
        try {
            if (buildPropertiesFile.canRead()) {
                InputStream is = new BufferedInputStream(new FileInputStream(buildPropertiesFile));
                try {
                    props.load(is);
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            getLog().warn("Exception reading build.properties file", e);
        }
        return props;
    }
}
