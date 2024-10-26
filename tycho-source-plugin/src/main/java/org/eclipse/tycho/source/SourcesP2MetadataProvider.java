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
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2resolver.AttachedArtifact;
import org.eclipse.tycho.resolver.P2MetadataProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("SourcesP2MetadataProvider")
public class SourcesP2MetadataProvider implements P2MetadataProvider {

    private final DependencyMetadataGenerator sourcesGenerator;
    private final BuildPropertiesParser buildPropertiesParser;

    @Inject
    public SourcesP2MetadataProvider(@Named(DependencyMetadataGenerator.SOURCE_BUNDLE) DependencyMetadataGenerator sourcesGenerator,
                                     BuildPropertiesParser buildPropertiesParser) {
        this.sourcesGenerator = sourcesGenerator;
        this.buildPropertiesParser = buildPropertiesParser;
    }

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
}
