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

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ArtifactDependencyWalker;

public abstract class AbstractArtifactBasedProject extends AbstractTychoProject {
    // this is stricter than Artifact.SNAPSHOT_VERSION
    public static final String SNAPSHOT_VERSION = "-SNAPSHOT";

    // requires resolved target platform
    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project) {
        return getDependencyWalker(project, null);
    }

    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        return newDependencyWalker(project, environment);
    }

    protected abstract ArtifactDependencyWalker newDependencyWalker(ReactorProject project,
            TargetEnvironment environment);

    protected String getOsgiVersion(ReactorProject project) {
        String version = project.getVersion();
        if (version.endsWith(SNAPSHOT_VERSION)) {
            version = version.substring(0, version.length() - SNAPSHOT_VERSION.length()) + ".qualifier";
        }
        return version;
    }
}
