/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

public class EmptyLifecycleExecutor implements LifecycleExecutor {

    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles(String packaging) {
        return null;
    }

    public MavenExecutionPlan calculateExecutionPlan(MavenSession session, String... tasks)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
            MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
            PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
            PluginVersionResolutionException {
        return null;
    }

    public void execute(MavenSession session) {
    }

    public void calculateForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
            PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
            LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
    }

    public List<MavenProject> executeForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws LifecycleExecutionException {
        return null;
    }

    public MavenExecutionPlan calculateExecutionPlan(MavenSession arg0, boolean arg1, String... arg2)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
            MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
            PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
            PluginVersionResolutionException {
        return null;
    }

}
