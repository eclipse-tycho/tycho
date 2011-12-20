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
package org.eclipse.tycho.artifacts;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;

/**
 * The result from resolving a project's dependencies against the target platform.
 */
public interface DependencyArtifacts {
    /**
     * Conventional qualifier used to denote "ANY QUALIFIER" in feature.xml and .product files. See
     * TYCHO-383.
     */
    public static final String ANY_QUALIFIER = "qualifier";

    /**
     * Returns all artifacts.
     */
    public List<ArtifactDescriptor> getArtifacts();

    /**
     * Returns all artifacts of the given type.
     */
    public List<ArtifactDescriptor> getArtifacts(String type);

    /**
     * Returns artifact of the given type and id and best matching version or null if no such
     * artifact is found.
     * <p>
     * This method uses the following version selection rules
     * <ul>
     * <li>0.0.0 or null matches the latest version
     * <li>1.2.3, i.e. without a qualifier, is equivalent to [1.2.3,1.2.4) and matches 1.2.3 with
     * the latest qualifier.
     * <li>1.2.3.qualifier, i.e. literal "qualifier", is equivalent to [1.2.3,1.2.4) and matches
     * 1.2.3 with the latest qualifier.
     * <li>all other versions match artifact with that exact version, 1.2.3.foo is equivalent to
     * [1.2.3.foo]
     * </ul>
     */
    public ArtifactDescriptor getArtifact(String type, String id, String version);

    public ReactorProject getMavenProject(File location);

    /**
     * Returns map of artifact descriptors at the given location. The map is keyed by maven artifact
     * classifiers. For dependency artifacts and the main reactor project artifact, the classifier
     * is <code>null</code>.
     */
    public Map<String, ArtifactDescriptor> getArtifact(File location);

    public ArtifactDescriptor getArtifact(ArtifactKey key);

    /**
     * Set of IInstallableUnits in the resolved project dependencies that come from outside the
     * local reactor, or <code>null</code> if the the project dependencies were not resolved from a
     * p2 target platform.<br/>
     * 
     * @return Set&lt;IInstallableUnit&gt; or null
     */
    public Set<?/* IInstallableUnit */> getNonReactorUnits();

    /**
     * For debug purposes only, do not use.
     * 
     * TODO move this out of here
     */
    public void toDebugString(StringBuilder sb, String linePrefix);
}
