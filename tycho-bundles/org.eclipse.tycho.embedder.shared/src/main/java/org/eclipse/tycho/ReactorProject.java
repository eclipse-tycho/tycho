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
 *    Christoph Läubrich - add getName() / combine directories / getter for Interpolator
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;
import java.util.Objects;

/**
 * A Tycho project in the reactor.
 */
public interface ReactorProject extends IDependencyMetadata {

    static final String CTX_INTERPOLATOR = "tycho.project.interpolator";
    static final String CTX_BUILDPROPERTIESPARSER = "tycho.project.buildpropertiesparser";

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

    // misc
    /**
     * 
     * @return the Interpolator for this project that could be used to resolve maven variable
     *         references
     */
    default Interpolator getInterpolator() {
        return Objects.requireNonNull((Interpolator) getContextValue(CTX_INTERPOLATOR),
                "No Interpolator found, has the TychoMavenLifecycleParticipant not run?");
    }

    default BuildProperties getBuildProperties() {
        BuildPropertiesParser parser = Objects.requireNonNull(
                (BuildPropertiesParser) getContextValue(CTX_BUILDPROPERTIESPARSER),
                "No BuildPropertiesParser found, has the TychoMavenLifecycleParticipant not run?");
        //we must always ask the parser here, it is expected that the parser caches the properties if the have not changed in the meanwhile
        return parser.parse(this);
    }

    /**
     * human-readable id used in error messages
     */
    public String getId();

    public boolean sameProject(/* MavenProject */Object otherProject);

    public String getName();
}
