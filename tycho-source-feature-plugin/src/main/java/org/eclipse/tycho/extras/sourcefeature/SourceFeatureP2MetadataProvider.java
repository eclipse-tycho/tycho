/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.p2.facade.internal.AttachedArtifact;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLDeclaration;

@Component(role = P2MetadataProvider.class, hint = "org.eclipse.tycho.extras.sourcefeature.SourceFeatureP2MetadataProvider")
public class SourceFeatureP2MetadataProvider implements P2MetadataProvider, Initializable {
    @Requirement
    private Logger log;

    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator generator;

    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        File template = new File(project.getBasedir(), SourceFeatureMojo.FEATURE_TEMPLATE_DIR);

        if (!ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || !template.isDirectory()) {
            return null;
        }

        Plugin plugin = project.getPlugin("org.eclipse.tycho.extras:tycho-source-feature-plugin");
        if (plugin != null) {
            try {
                File sourceFeatureBasedir = SourceFeatureMojo.getSourcesFeatureDir(project);

                /*
                 * There is no easy way to determine what *exact* source bundles/features will be
                 * included in the source feature at this point. Because of this, the source feature
                 * dependency-only metadata does not include any dependencies.
                 * 
                 * This has two implications.
                 * 
                 * First, any missing source bundles/features will not be detected/reported until
                 * source feature mojo is executed. This is inconsistent with how everything else
                 * works in Tycho, but probably is a good thing.
                 * 
                 * More importantly, though, source bundles/features are not included as transitive
                 * dependencies of other reactor projects that include the source feature. To solve
                 * this for eclipse-repository project, repository project dependencies are
                 * recalculated during repository packaging. Other 'aggregating' project types, like
                 * eclipse-update-site and eclipse-feature with deployableFeature=true, will not be
                 * compatible with source features until
                 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=353889 is implemented.
                 */
                Feature feature = Feature.read(new File(project.getBasedir(), "feature.xml"));

                Document document = new Document();
                document.setRootNode(new Element("feature"));
                document.setXmlDeclaration(new XMLDeclaration("1.0", "UTF-8"));
                Feature sourceFeature = new Feature(document);

                sourceFeature.setId(feature.getId() + ".source");
                sourceFeature.setVersion(feature.getVersion());

                Feature.write(sourceFeature, new File(sourceFeatureBasedir, Feature.FEATURE_XML));

                String classifier = SourceFeatureMojo.SOURCES_FEATURE_CLASSIFIER;
                IArtifactFacade artifact = new AttachedArtifact(project, sourceFeatureBasedir, classifier);
                return Collections.singletonMap(classifier,
                        generator.generateMetadata(artifact, null, OptionalResolutionAction.REQUIRE));
            } catch (IOException e) {
                log.error("Could not create sources feature.xml", e);
            }
        }

        return null;
    }

    public void initialize() throws InitializationException {
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
    }

}
