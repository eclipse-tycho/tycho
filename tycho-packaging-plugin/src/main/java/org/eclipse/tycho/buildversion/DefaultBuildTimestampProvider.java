/*******************************************************************************
 * Copyright (c) 2012, 2016 Sonatype Inc. and others.
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
 * Build timestamp provider that returns the same timestamp for all projects, the
 * ${maven.build.timestamp}.
 */
@Component(role = BuildTimestampProvider.class, hint = DefaultBuildTimestampProvider.ROLE_HINT)
public class DefaultBuildTimestampProvider implements BuildTimestampProvider {

    static final String ROLE_HINT = "default";

    @Override
    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution) {
        return session.getStartTime();
    }
}
