/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc (mistria) - extracted interface from BaselineValidator
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.p2.metadata.IP2Artifact;

/**
 * This interface is used to provide various strategies for comparison and replacement of build
 * artifacts with ones from baseline. Strategies may register as {@link Component} using this
 * interface as key, and specifying the hint of their choice. Users are free to choose the strategy
 * of their choice by setting the "baselineStrategy" parameter of tycho-p2-plugin.
 * 
 */
public interface BaselineValidator {

    public abstract Map<String, IP2Artifact> validateAndReplace(MavenProject project,
            Map<String, IP2Artifact> reactorMetadata, List<Repository> baselineRepositories, BaselineMode baselineMode,
            BaselineReplace baselineReplace) throws IOException, MojoExecutionException;

}
