/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = BuildTimestampProvider.class, hint = "test")
public class TestBuildTimestampProvider implements BuildTimestampProvider {

    public static final String PROP_TESTBUILDTIMESTAMPE = "testbuildtimestamp";

    private final SimpleDateFormat format;

    public TestBuildTimestampProvider() {
        format = new SimpleDateFormat("yyyyMMddHHmm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution)
            throws MojoExecutionException {
        String prop = session.getCurrentProject().getProperties().getProperty(PROP_TESTBUILDTIMESTAMPE);

        if (prop == null) {
            throw new IllegalArgumentException("Build timestamp property is not set.");
        }

        try {
            return format.parse(prop);
        } catch (ParseException e) {
            throw new MojoExecutionException("Could not parse build timestamp", e);
        }
    }

}
