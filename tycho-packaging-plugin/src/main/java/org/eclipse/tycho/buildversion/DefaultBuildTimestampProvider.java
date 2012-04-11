/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.util.Date;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Build timestamp provider that returns the same timestamp for all projects. The timestamp is kept
 * as a session-wide property and equals to the first time the build timestamp is requested.
 * 
 * @TODO use ${maven.build.timestamp}. https://bugs.eclipse.org/bugs/show_bug.cgi?id=367945
 */
@Component(role = BuildTimestampProvider.class, hint = DefaultBuildTimestampProvider.ROLE_HINT)
public class DefaultBuildTimestampProvider implements BuildTimestampProvider {

    static final String ROLE_HINT = "default";

    private static final String REACTOR_BUILD_TIMESTAMP_PROPERTY = "reactorBuildTimestampProperty";

    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution) {
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
