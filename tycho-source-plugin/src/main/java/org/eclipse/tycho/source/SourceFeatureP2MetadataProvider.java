/*******************************************************************************
 * Copyright (c) 2011, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 568359 - move tycho-extras SourceFeatureMojo to tycho-source-feature
 *******************************************************************************/
package org.eclipse.tycho.source;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2resolver.AttachedArtifact;
import org.eclipse.tycho.resolver.P2MetadataProvider;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("org.eclipse.tycho.source.SourceFeatureP2MetadataProvider")
public class SourceFeatureP2MetadataProvider implements P2MetadataProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DependencyMetadataGenerator generator;

    @Inject
    public SourceFeatureP2MetadataProvider(@Named(DependencyMetadataGenerator.DEPENDENCY_ONLY) DependencyMetadataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        if (!SourceFeatureMojo.isEnabledForProject(project)) {
            return null;
        }
        try {
            File sourceFeatureBasedir = SourceFeatureMojo.getSourcesFeatureOutputDir(project);
            Feature sourceFeature = createPreliminarySourceFeature(project);
            Feature.write(sourceFeature, new File(sourceFeatureBasedir, Feature.FEATURE_XML));
            String classifier = SourceFeatureMojo.SOURCES_FEATURE_CLASSIFIER;
            IArtifactFacade artifact = new AttachedArtifact(project, sourceFeatureBasedir, classifier);
            return Collections.singletonMap(classifier, generator.generateMetadata(artifact, null,
                    OptionalResolutionAction.REQUIRE, new PublisherOptions()));
        } catch (IOException e) {
            logger.error("Could not create sources feature.xml", e);
            return null;
        }

    }

    static Feature createPreliminarySourceFeature(MavenProject project) throws IOException {
        /*
         * There is no easy way to determine what *exact* source bundles/features will be included
         * in the source feature at this point. Because of this, the source feature dependency-only
         * metadata does not include any dependencies.
         * 
         * This has two implications.
         * 
         * First, any missing source bundles/features will not be detected/reported until source
         * feature mojo is executed. This is inconsistent with how everything else works in Tycho,
         * but probably is a good thing.
         * 
         * More importantly, though, source bundles/features are not included as transitive
         * dependencies of other reactor projects that include the source feature. To solve this for
         * eclipse-repository project, repository project dependencies are recalculated during
         * repository packaging. Other 'aggregating' project types, like eclipse-feature with
         * deployableFeature=true, will not be compatible with source features until
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=353889 is implemented.
         */
        Feature feature = Feature.read(new File(project.getBasedir(), "feature.xml"));

        Document document = new Document();
        document.setRootNode(new Element("feature"));
        document.setXmlDeclaration(new XMLDeclaration("1.0", "UTF-8"));
        Feature sourceFeature = new Feature(document);

        sourceFeature.setId(feature.getId() + ".source");
        sourceFeature.setVersion(feature.getVersion());

        // 410418 source feature includes binary feature
        FeatureRef binaryRef = new FeatureRef("includes");
        binaryRef.setId(feature.getId());
        binaryRef.setVersion(feature.getVersion());
        sourceFeature.addFeatureRef(binaryRef);
        return sourceFeature;
    }
}
