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
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;

/**
 * @phase package
 * @goal package-feature
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class PackageFeatureMojo extends AbstractTychoPackagingMojo {

    /**
     * The maven archiver to use.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * @parameter expression="${project.basedir}"
     */
    private File basedir;

    /**
     * Name of the generated JAR.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * If set to <code>true</code> (the default), standard eclipse update site directory with
     * feature content will be created under target folder.
     * 
     * @parameter default-value="false"
     */
    private boolean deployableFeature = false;

    /**
     * @parameter expression="${project.build.directory}/site"
     */
    private File target;

    /**
     * @component
     */
    private FeatureXmlTransformer featureXmlTransformer;

    public void execute() throws MojoExecutionException, MojoFailureException {
        expandVersion();

        Properties props = new Properties();
        try {
            FileInputStream is = new FileInputStream(new File(basedir, "build.properties"));
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading build properties", e);
        }

        outputDirectory.mkdirs();

        Feature feature;
        File featureXml = new File(outputDirectory, Feature.FEATURE_XML);
        try {
            feature = getUpdatedFeatureXml();
            Feature.write(feature, featureXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Error updating feature.xml", e);
        }

        File outputJar = new File(outputDirectory, finalName + ".jar");
        outputJar.getParentFile().mkdirs();
        List<String> binIncludes = toFilePattern(props.getProperty("bin.includes"));
        List<String> binExcludes = toFilePattern(props.getProperty("bin.excludes"));
        binExcludes.add(Feature.FEATURE_XML); // we'll include updated feature.xml

        MavenArchiver archiver = new MavenArchiver();
        JarArchiver jarArchiver = getJarArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJar);
        jarArchiver.setDestFile(outputJar);

        try {
            archiver.getArchiver().addFileSet(getFileSet(basedir, binIncludes, binExcludes));
            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);
            archiver.createArchive(project, archive);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating feature package", e);
        }

        project.getArtifact().setFile(outputJar);

        if (deployableFeature) {
            assembleDeployableFeature(feature);
        }
    }

    private void assembleDeployableFeature(Feature feature) throws MojoExecutionException {
        UpdateSiteAssembler assembler = new UpdateSiteAssembler(session, target);
        getDependencyWalker().walk(assembler);
    }

    private Feature getUpdatedFeatureXml() throws MojoExecutionException, IOException {
        return featureXmlTransformer.transform(DefaultReactorProject.adapt(project), Feature.loadFeature(basedir),
                getDependencyWalker());
    }

    private JarArchiver getJarArchiver() throws MojoExecutionException {
        try {
            return (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Unable to get JarArchiver", e);
        }
    }
}
