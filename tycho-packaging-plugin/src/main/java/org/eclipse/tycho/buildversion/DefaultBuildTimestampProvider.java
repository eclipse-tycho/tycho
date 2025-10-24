/*******************************************************************************
 * Copyright (c) 2012, 2016 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.build.BuildTimestampProvider;

/**
 * Build timestamp provider that returns the same timestamp for all projects. If
 * the standard Maven property ${project.build.outputTimestamp} exists, its
 * value is used for the timestamp. If it does not exist (or cannot be parsed),
 * the ${maven.build.timestamp} timestamp is used instead.
 */
@Singleton
@Named(DefaultBuildTimestampProvider.ROLE_HINT)
public class DefaultBuildTimestampProvider implements BuildTimestampProvider {

    static final String ROLE_HINT = "default";

    @Override
    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution) {
		// Use the outputTimestamp property value if available for reproducible builds
		final String outputTimestamp = (String) project.getProperties().get("project.build.outputTimestamp");
		Optional<Instant> reproducibleTimestamp = MavenArchiver.parseBuildOutputTimestamp(outputTimestamp);
		if (reproducibleTimestamp.isPresent()) {
			return Date.from(reproducibleTimestamp.get());
		} else {
			return session.getStartTime();
		}
    }

	@Override
	public void setQuiet(boolean quiet) {

	}
}
