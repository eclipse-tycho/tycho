/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.build;

import java.util.Date;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public interface BuildTimestampProvider {

    /**
     * Returns build timestamp of the current project.
     */
    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution)
            throws MojoExecutionException;

    /**
     * ask the provider to be as quiet as possible (e.g. not emit any message at INFO / WARNING)
     * 
     * @param quiet
     */
    public void setQuiet(boolean quiet);

}
