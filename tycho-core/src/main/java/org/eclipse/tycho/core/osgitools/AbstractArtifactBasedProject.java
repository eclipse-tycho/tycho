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
package org.eclipse.tycho.core.osgitools;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

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

    protected abstract ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment);

    @Override
    public void checkForMissingDependencies(ReactorProject project) {
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);

        // this throws exceptions when dependencies are missing
        getDependencyWalker(project).walk(new ArtifactDependencyVisitor() {
        });
    }

    protected String getOsgiVersion(ReactorProject project) {
        String version = project.getVersion();
        if (version.endsWith(SNAPSHOT_VERSION)) {
            version = version.substring(0, version.length() - SNAPSHOT_VERSION.length()) + ".qualifier";
        }
        return version;
    }
}
