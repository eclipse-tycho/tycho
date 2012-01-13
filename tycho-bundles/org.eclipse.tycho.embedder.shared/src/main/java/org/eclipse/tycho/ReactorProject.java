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
package org.eclipse.tycho;

import java.io.File;
import java.util.Map;
import java.util.Set;

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

    // build configuration

    public File getOutputDirectory();

    public File getBuildDirectory();

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

    /**
     * Returns live set of Manve artifact classifiers that have been (or will be) attached to the
     * project during the build. Main project artifact classifier is always
     * <code>null</null> and is NOT included in the returned set.
     */
    // public Set<String> getClassifiers();

    public void setDependencyMetadata(String classifier, boolean primary,
            Set<Object /* IInstallableUnit */> installableUnits);

    /**
     * Returns live set of P2 IInstallableUnit's that describe requirements and provided
     * capabilities of this project used during project dependency resolution for the specified
     * classifier.
     */
    public Set<Object /* IInstallableUnit */> getDependencyMetadata(String classifier, boolean primary);

    /**
     * Returns project dependency metadata.
     * <p/>
     * Returned map is keyed by project attached artifact classifier, where <code>null</code>
     * represents main the project artifact. Map values are sets of both primary and secondary
     * project installable units.
     */
    public Map<String, Set<Object>> getDependencyMetadata();

    /**
     * Per-classifier project P2 metadata. This is complete P2 metadata, as generated by P2
     * publisher application (or equivalent).
     */
    // public Set<Object /* IInstallableUnit */> getRuntimeMetadata( String classifier );

    public String getExpandedVersion();

    public void setExpandedVersion(String originalVersion, String qualifier);

    // misc

    /**
     * human-readable id used in error messages
     */
    public String getId();

    public boolean sameProject(/* MavenProject */Object otherProject);
}
