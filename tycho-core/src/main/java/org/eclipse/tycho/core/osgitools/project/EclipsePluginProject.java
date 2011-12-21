/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.project;

import java.util.List;
import java.util.Map;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.BuildProperties;

public interface EclipsePluginProject {

    public ReactorProject getMavenProject();

    public BuildProperties getBuildProperties();

    public List<BuildOutputJar> getOutputJars();

    public BuildOutputJar getDotOutputJar();

    public Map<String, BuildOutputJar> getOutputJarMap();
}
