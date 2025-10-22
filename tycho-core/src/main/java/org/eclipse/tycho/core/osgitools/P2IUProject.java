/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.model.IU;

@Named(org.eclipse.tycho.ArtifactType.TYPE_INSTALLABLE_UNIT)
@Singleton
public class P2IUProject extends AbstractArtifactBasedProject {
    @Override
    protected ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        IU anIU = IU.loadIU(project.getBasedir());
        return new DefaultArtifactKey(org.eclipse.tycho.ArtifactType.TYPE_INSTALLABLE_UNIT, anIU.getId(),
                anIU.getVersion());
    }

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        super.setupProject(session, project);
        //Load the XML here to fail as early as possible like it is done in EclipseFeatureProject
        IU.loadIU(project.getBasedir());
    }

}
