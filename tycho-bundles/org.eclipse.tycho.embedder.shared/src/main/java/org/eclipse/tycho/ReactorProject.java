/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - add getName() / combine directories
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;

/**
 * A Tycho project in the reactor.
 */
public interface ReactorProject extends IDependencyMetadata {

    /**
     * Conventional sources jar Maven artifact classifier.
     */
    public static final String SOURCE_ARTIFACT_CLASSIFIER = "sources";

    public static final String CTX_REQUIREMENTS = "tycho.requirements";

    public File getBasedir();

    public String getPackaging();

    // Maven coordinates

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public ReactorProjectIdentities getIdentities();

    // build configuration

    public BuildDirectory getBuildDirectory();

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

    public String getBuildQualifier();

    public String getExpandedVersion();

    public Requirements getRequirements();

    // misc

    /**
     * human-readable id used in error messages
     */
    public String getId();

    public boolean sameProject(/* MavenProject */Object otherProject);

    public String getName();
}
