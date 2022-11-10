/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.core;

import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;

public interface BundleProject extends TychoProject {
    public List<ClasspathEntry> getClasspath(ReactorProject project);

    public List<AccessRule> getBootClasspathExtraAccessRules(ReactorProject project);

    /**
     * Returns the value of the specified attribute key in the project's MANIFEST, or null if the
     * attribute was not found.
     * 
     * @param key
     *            manifest attribute key
     * @param project
     *            associated maven project
     * @return the String value of the specified attribute key, or null if not found.
     */
    public String getManifestValue(String key, MavenProject project);

    public List<ArtifactKey> getExtraTestRequirements(ReactorProject project);

    public List<ClasspathEntry> getTestClasspath(ReactorProject project);

    public List<ClasspathEntry> getTestClasspath(ReactorProject project, boolean complete);

    /**
     * @return a collection of dependencies that where present before Tycho has injected the target
     *         content of the project into the model
     */
    Map<Dependency, Artifact> getInitialArtifactMap(ReactorProject reactorProject);

}
