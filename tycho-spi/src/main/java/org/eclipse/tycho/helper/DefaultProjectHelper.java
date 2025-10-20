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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Named
public class DefaultProjectHelper implements ProjectHelper {

    @Inject
    private MojoDescriptorCreator mojoDescriptorCreator;

    @Inject
    private LegacySupport legacySupport;

    private Map<String, Plugin> cliPlugins = new ConcurrentHashMap<String, Plugin>();

    /**
     * Get all plugins for a project, either configured directly or specified on the commandline
     * 
     * @param project
     * @param mavenSession
     * @return
     */
    public List<Plugin> getPlugins(MavenProject project, MavenSession mavenSession) {
        List<Plugin> plugins = new ArrayList<Plugin>(project.getBuildPlugins());
        for (String goal : mavenSession.getGoals()) {
            if (goal.indexOf(':') >= 0) {
                Plugin plugin = cliPlugins.computeIfAbsent(goal, cli -> {
                    try {
                        MojoDescriptor mojoDescriptor = mojoDescriptorCreator.getMojoDescriptor(goal, mavenSession,
                                project);
                        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
                        Plugin p = pluginDescriptor.getPlugin();
                        PluginExecution execution = new PluginExecution();
                        execution.setId("default-cli");
                        execution.addGoal(mojoDescriptor.getGoal());
                        p.addExecution(execution);
                        return p;
                    } catch (Exception e) {
                        return null;
                    }
                });
                if (plugin != null) {
                    plugins.add(plugin);
                }
            }
        }
        return plugins;
    }

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
    public boolean hasPluginExecution(String pluginGroupId, String pluginArtifactId, String goal, MavenProject project,
            MavenSession mavenSession) {
        MavenSession clone = mavenSession.clone();
        clone.setCurrentProject(project);
        List<Plugin> plugins = getPlugins(project, clone);
        for (Plugin plugin : plugins) {
            if (plugin.getGroupId().equals(pluginGroupId) && plugin.getArtifactId().equals(pluginArtifactId)) {
                for (PluginExecution execution : plugin.getExecutions()) {
                    if (execution.getGoals().contains(goal)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Xpp3Dom getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String goal) {
        MavenSession currentSession = legacySupport.getSession();
        if (currentSession == null) {
            return null;
        }
        MavenProject currentProject = currentSession.getCurrentProject();
        if (currentProject == null) {
            return null;
        }
        return getPluginConfiguration(pluginGroupId, pluginArtifactId, goal, currentProject, currentSession);
    }

    public Xpp3Dom getPluginConfiguration(String pluginGroupId, String pluginArtifactId, String goal,
            MavenProject project, MavenSession mavenSession) {
        MavenSession clone = mavenSession.clone();
        clone.setCurrentProject(project);
        List<Plugin> plugins = getPlugins(project, clone);
        for (Plugin plugin : plugins) {
            if (plugin.getGroupId().equals(pluginGroupId) && plugin.getArtifactId().equals(pluginArtifactId)) {
                if (goal == null) {
                    return getDom(plugin.getConfiguration());
                }
                //first check for goal specific configuration
                for (PluginExecution execution : plugin.getExecutions()) {
                    if (execution.getGoals().contains(goal)) {
                        return getDom(execution.getConfiguration());
                    }
                }
                //get plugin config
                return getDom(plugin.getConfiguration());
            }
        }
        return null;
    }

    public MavenProject getCurrentProject() {
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            return null;
        }
        return session.getCurrentProject();
    }

    public Xpp3Dom getDom(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Xpp3Dom xpp3) {
            return xpp3;
        }
        try {
            return Xpp3DomBuilder.build(new StringReader(object.toString()));
        } catch (XmlPullParserException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
