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
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.p2.facade.internal.ReactorArtifactFacade;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator.OptionalResolutionAction;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class, hint = "SourcesP2MetadataProvider")
public class SourcesP2MetadataProvider implements P2MetadataProvider, Initializable {

    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator sourcesGenerator;

    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        if (isBundleProject(project) && hasSourceBundle(project)) {
            ReactorArtifactFacade sourcesArtifact = new ReactorArtifactFacade(reactorProject, "sources");
            IDependencyMetadata metadata = sourcesGenerator.generateMetadata(sourcesArtifact, null,
                    OptionalResolutionAction.REQUIRE);
            reactorProject.setDependencyMetadata(sourcesArtifact.getClassidier(), false, metadata.getMetadata());
        }
    }

    public void initialize() throws InitializationException {
        this.sourcesGenerator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=source-bundle)");
    }

    private static boolean isBundleProject(MavenProject project) {
        String type = project.getPackaging();
        return ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(type);
    }

    private static boolean hasSourceBundle(MavenProject project) {
        // TODO this is a fragile way of checking whether we generate a source bundle
        // should we rather use MavenSession to get the actual configured mojo instance?
        for (Plugin plugin : project.getBuildPlugins()) {
            if ("org.eclipse.tycho:tycho-source-plugin".equals(plugin.getKey())) {
                return true;
            }
        }
        return false;
    }

}
