/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;

public interface ProviderHelper {

    ProviderSelection selectProvider(MavenProject project, List<ClasspathEntry> classpath,
            Properties providerProperties, String providerHint) throws MojoExecutionException;

    Set<Artifact> filterTestFrameworkBundles(TestFrameworkProvider provider, List<Artifact> pluginArtifacts)
            throws MojoExecutionException;

    List<String> getSymbolicNames(Set<Artifact> bundleArtifacts);

}
