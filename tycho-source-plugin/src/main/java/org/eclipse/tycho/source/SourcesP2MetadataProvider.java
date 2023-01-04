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
package org.eclipse.tycho.source;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2resolver.AttachedArtifact;
import org.eclipse.tycho.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class, hint = "SourcesP2MetadataProvider")
public class SourcesP2MetadataProvider implements P2MetadataProvider, Initializable {

    @Requirement(hint = DependencyMetadataGenerator.SOURCE_BUNDLE)
    private DependencyMetadataGenerator sourcesGenerator;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        if (OsgiSourceMojo.isRelevant(project, buildPropertiesParser)) {
            IArtifactFacade sourcesArtifact = new AttachedArtifact(project, project.getBasedir(), "sources");
            return Collections.singletonMap(sourcesArtifact.getClassifier(), sourcesGenerator
                    .generateMetadata(sourcesArtifact, null, OptionalResolutionAction.REQUIRE, new PublisherOptions()));
        }
        return null;
    }

    @Override
    public void initialize() throws InitializationException {
    }
}
