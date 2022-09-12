/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.core.osgitools.project;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.dotClasspath.ProjectClasspathEntry;

public interface EclipsePluginProject {

    public ReactorProject getMavenProject();

    public BuildProperties getBuildProperties();

    public List<BuildOutputJar> getOutputJars();

    public Collection<ProjectClasspathEntry> getClasspathEntries();

    public BuildOutputJar getDotOutputJar();

    public Map<String, BuildOutputJar> getOutputJarMap();
}
