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
package org.eclipse.tycho.source;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.facade.internal.ReactorArtifactFacade;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class, hint = "SourcesP2MetadataProvider")
public class SourcesP2MetadataProvider implements P2MetadataProvider, Initializable {

    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator sourcesGenerator;

    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        if (OsgiSourceMojo.isRelevantProjectImpl(project)) {
            ReactorArtifactFacade sourcesArtifact = new ReactorArtifactFacade(reactorProject, "sources");
            IDependencyMetadata metadata = sourcesGenerator.generateMetadata(sourcesArtifact, null,
                    OptionalResolutionAction.REQUIRE);
            reactorProject.setDependencyMetadata(sourcesArtifact.getClassidier(), false, metadata.getMetadata());
        }
    }

    public void initialize() throws InitializationException {
        this.sourcesGenerator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=source-bundle)");
    }
}
