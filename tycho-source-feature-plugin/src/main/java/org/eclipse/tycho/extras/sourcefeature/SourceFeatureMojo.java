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
import java.util.Map;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.AbstractArtifactDependencyWalker;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.packaging.FeatureXmlTransformer;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLDeclaration;

/**
 * @goal source-feature
 */
public class SourceFeatureMojo extends AbstractMojo {

    public static final String FEATURE_TEMPLATE_DIR = "sourceTemplateFeature";

    public static final String SOURCES_FEATURE_CLASSIFIER = "sources-feature";

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @component
     */
    private FeatureXmlTransformer featureXmlSomething;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File template = new File(project.getBasedir(), FEATURE_TEMPLATE_DIR);

        if (!ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || !template.isDirectory()) {
            return;
        }

        File outputJarFile = getOutputJarFile();

        TychoProject tychoProject = projectTypes.get(project.getPackaging());

        if (tychoProject == null) {
            throw new MojoExecutionException("Is not a supported tycho project " + project);
        }

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJarFile);

        try {
            File featureXml = getSourcesFeatureXml(project);

            DependencyArtifacts dependencies = tychoProject.getDependencyArtifacts(project);

            final Feature sourceFeature = getSourceFeature(project);

            AbstractArtifactDependencyWalker walker = new AbstractArtifactDependencyWalker(dependencies, null) {
                public void walk(ArtifactDependencyVisitor visitor) {
                    traverseFeature(project.getBasedir(), sourceFeature, visitor);
                }
            };

            Feature feature = featureXmlSomething
                    .transform(DefaultReactorProject.adapt(project), sourceFeature, walker);
            Feature.write(feature, featureXml);

            DefaultFileSet mainFileSet = new DefaultFileSet();
            mainFileSet.setDirectory(template);

            archiver.getArchiver().addFileSet(mainFileSet);

            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);

            archiver.createArchive(project, archive);

            projectHelper.attachArtifact(project, outputJarFile, SOURCES_FEATURE_CLASSIFIER);

        } catch (Exception e) {
            throw new MojoExecutionException("Could not package source feature jar", e);
        }
    }

    static File getSourcesFeatureXml(MavenProject project) {
        File featureXml = new File(project.getBuild().getDirectory(), "source-feature/feature.xml");
        featureXml.getParentFile().mkdirs();
        return featureXml;
    }

    static Feature getSourceFeature(MavenProject project) throws IOException {
        Feature feature = Feature.read(new File(project.getBasedir(), "feature.xml"));

        Document document = new Document();
        document.setRootNode(new Element("feature"));
        document.setXmlDeclaration(new XMLDeclaration("1.0", "UTF-8"));
        Feature sourceFeature = new Feature(document);
        sourceFeature.setId(feature.getId() + ".source");
        sourceFeature.setVersion(feature.getVersion());

        for (PluginRef pluginRef : feature.getPlugins()) {
            PluginRef sourcePluginRef = new PluginRef("plugin");

            // TODO lookup in target platform
            sourcePluginRef.setId(pluginRef.getId() + ".source");
            sourcePluginRef.setVersion(pluginRef.getVersion());
            sourcePluginRef.setDownloadSide(0);
            sourcePluginRef.setInstallSize(0);
            sourcePluginRef.setUnpack(false);

            sourceFeature.addPlugin(sourcePluginRef);
        }
        return sourceFeature;
    }

    protected File getOutputJarFile() {
        String filename = project.getArtifactId() + "-" + SOURCES_FEATURE_CLASSIFIER + ".jar";
        return new File(project.getBuild().getDirectory(), filename);
    }

}
