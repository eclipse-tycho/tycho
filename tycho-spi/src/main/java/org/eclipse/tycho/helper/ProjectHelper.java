/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.helper;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public interface ProjectHelper {

    /**
     * Get all plugins for a project, either configured directly or specified on the commandline
     * 
     * @param project
     * @param mavenSession
     * @return
     */
    List<Plugin> getPlugins(MavenProject project, MavenSession mavenSession);

    /**
     * Check if there is at least one plugin execution configured for the specified plugin and goal
     * 
     * @param pluginGroupId
     * @param pluginArtifactId
     * @param goal
     * @param project
     * @param mavenSession
     * @return <code>true</code> if an execution was found or <code>false</code> otherwise.
     */
    boolean hasPluginExecution(String pluginGroupId, String pluginArtifactId, String goal, MavenProject project,
            MavenSession mavenSession);

    Xpp3Dom getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String goal);

    Xpp3Dom getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String goal,
            MavenProject project, MavenSession mavenSession);

    MavenProject getCurrentProject();

    Xpp3Dom getDom(Object object);

}
