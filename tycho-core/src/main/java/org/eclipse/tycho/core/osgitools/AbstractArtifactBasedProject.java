/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.core.osgitools;

import org.apache.maven.plugin.LegacySupport;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;

public abstract class AbstractArtifactBasedProject extends AbstractTychoProject {
    // this is stricter than Artifact.SNAPSHOT_VERSION
    public static final String SNAPSHOT_VERSION = TychoConstants.SUFFIX_SNAPSHOT;

    public AbstractArtifactBasedProject(MavenDependenciesResolver projectDependenciesResolver, LegacySupport legacySupport, TychoProjectManager projectManager, DependencyResolver dependencyResolver) {
        super(projectDependenciesResolver, legacySupport, projectManager, dependencyResolver);
    }

    // requires resolved target platform
    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project) {
        return getDependencyWalker(project, null);
    }

    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        return newDependencyWalker(project, environment);
    }

    protected abstract ArtifactDependencyWalker newDependencyWalker(ReactorProject project,
            TargetEnvironment environment);

    protected String getOsgiVersion(ReactorProject project) {
        String version = project.getVersion();
        if (version.endsWith(SNAPSHOT_VERSION)) {
            version = version.substring(0, version.length() - SNAPSHOT_VERSION.length())
                    + TychoConstants.SUFFIX_QUALIFIER;
        }
        return version;
    }
}
