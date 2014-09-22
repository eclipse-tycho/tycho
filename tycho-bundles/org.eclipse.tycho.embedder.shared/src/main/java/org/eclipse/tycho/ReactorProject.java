/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;
import java.util.Set;

/**
 * A Tycho project in the reactor.
 */
public interface ReactorProject {
    /**
     * Conventional key used to store ReactorProject in MavenProject.context
     */
    public static final String CTX_REACTOR_PROJECT = "tycho.reactor-project";

    /**
     * Conventional key used to store dependency metadata in MavenProject.context
     */
    public static final String CTX_DEPENDENCY_METADATA = "tycho.dependency-metadata";

    /**
     * Conventional key used to store secondary dependency metadata in MavenProject.context
     */
    public static final String CTX_SECONDARY_DEPENDENCY_METADATA = "tycho.secondary-dependency-metadata";

    /**
     * Conventional sources jar Maven artifact classifier.
     */
    public static final String SOURCE_ARTIFACT_CLASSIFIER = "sources";

    public File getBasedir();

    public String getPackaging();

    // Maven coordinates

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public ReactorProjectIdentities getIdentities();

    // build configuration

    public File getOutputDirectory();

    public BuildOutputDirectory getBuildDirectory();

    public File getTestOutputDirectory();

    // attached artifacts

    /**
     * Returns main project artifact file or null, if the project has not been packaged yet.
     */
    public File getArtifact();

    /**
     * returns attached artifact file or null if no such attached artifact.
     */
    public File getArtifact(String artifactClassifier);

    // context values

    public Object getContextValue(String key);

    public void setContextValue(String key, Object value);

    //

    public void setDependencyMetadata(boolean primary, Set<? /* IInstallableUnit */> installableUnits);

    /**
     * Returns set of p2 <tt>IInstallableUnit</tt>s that describe requirements and provided
     * capabilities of this project.
     */
    public Set<?> getDependencyMetadata(boolean primary);

    /**
     * Returns project dependency metadata with both primary and secondary project installable
     * units.
     */
    public Set<?> getDependencyMetadata();

    public String getExpandedVersion();

    public void setExpandedVersion(String originalVersion, String qualifier);

    // misc

    /**
     * human-readable id used in error messages
     */
    public String getId();

    public boolean sameProject(/* MavenProject */Object otherProject);

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();
}
