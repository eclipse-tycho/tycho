/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;

@Mojo(name = "package-feature", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageFeatureMojo extends AbstractTychoPackagingMojo {

    private static final String FEATURE_PROPERTIES = "feature.properties";

    /**
     * The <a href="http://maven.apache.org/shared/maven-archiver/">maven archiver</a> to use. One
     * of the archiver properties is the <code>addMavenDescriptor</code> flag, which indicates
     * whether the generated archive will contain the pom.xml and pom.properties file. If no archive
     * configuration is specified, the default value is <code>false</code>. If the maven descriptor
     * should be added to the artifact, use the following configuration:
     * 
     * <pre>
     * &lt;plugin&gt;
     *   &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *   &lt;artifactId&gt;tycho-packaging-plugin&lt;/artifactId&gt;
     *   &lt;version&gt;${tycho-version}&lt;/version&gt;
     *   &lt;configuration&gt;
     *     &lt;archive&gt;
     *       &lt;addMavenDescriptor&gt;true&lt;/addMavenDescriptor&gt;
     *     &lt;/archive&gt;
     *   &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     */
    @Parameter
    private MavenArchiveConfiguration archive;

    /**
     * The output directory of the jar file
     * 
     * By default this is the Maven <tt>target/</tt> directory.
     */
    @Parameter(property = "project.build.directory")
    private File outputDirectory;

    @Parameter(property = "project.basedir")
    private File basedir;

    /**
     * Name of the generated JAR.
     */
    @Parameter(property = "project.build.finalName", alias = "jarName", required = true)
    private String finalName;

    /**
     * If set to <code>true</code>, standard eclipse update site directory with feature content will
     * be created under target folder.
     */
    @Parameter(defaultValue = "false")
    private boolean deployableFeature = false;

    @Parameter(defaultValue = "${project.build.directory}/site")
    private File target;

    @Component
    private FeatureXmlTransformer featureXmlTransformer;

    @Component
    private LicenseFeatureHelper licenseFeatureHelper;

    @Component
    private BuildPropertiesParser buildPropertiesParser;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        outputDirectory.mkdirs();

        Feature feature = Feature.loadFeature(basedir);

        File licenseFeature = licenseFeatureHelper.getLicenseFeature(feature, project);

        // remove license feature id and version from feature.xml
        feature.setLicenseFeature(null);
        feature.setLicenseFeatureVersion(null);

        File featureXml = new File(outputDirectory, Feature.FEATURE_XML);
        try {
            expandVersionQualifiers(feature);
            Feature.write(feature, featureXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Error updating feature.xml", e);
        }

        BuildProperties buildProperties = buildPropertiesParser.parse(project.getBasedir());
        checkBinIncludesExist(buildProperties);

        File featureProperties = getFeatureProperties(licenseFeature, buildProperties);

        File outputJar = new File(outputDirectory, finalName + ".jar");
        outputJar.getParentFile().mkdirs();

        MavenArchiver archiver = new MavenArchiver();
        JarArchiver jarArchiver = getJarArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJar);
        jarArchiver.setDestFile(outputJar);

        try {
            archiver.getArchiver().addFileSet(getManuallyIncludedFiles(buildProperties));
            if (licenseFeature != null) {
                archiver.getArchiver()
                        .addArchivedFileSet(licenseFeatureHelper.getLicenseFeatureFileSet(licenseFeature));
            }
            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);
            if (featureProperties != null) {
                archiver.getArchiver().addFile(featureProperties, FEATURE_PROPERTIES);
            }
            if (archive == null) {
                archive = new MavenArchiveConfiguration();
                archive.setAddMavenDescriptor(false);
            }
            archiver.createArchive(session, project, archive);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating feature package", e);
        }

        project.getArtifact().setFile(outputJar);

        if (deployableFeature) {
            assembleDeployableFeature();
        }
    }

    private File getFeatureProperties(File licenseFeature, BuildProperties buildProperties)
            throws MojoExecutionException {
        File featureProperties = null;
        if (buildProperties.getBinIncludes().contains(FEATURE_PROPERTIES)) {
            featureProperties = new File(outputDirectory, FEATURE_PROPERTIES);
            if (featureProperties.exists() && !featureProperties.delete()) {
                throw new MojoExecutionException("Could not delete file " + featureProperties.getAbsolutePath());
            }

            OutputStream os = null;
            try {
                File localFeatureProperties = new File(basedir, FEATURE_PROPERTIES);

                if (localFeatureProperties.canRead()) {
                    os = new BufferedOutputStream(new FileOutputStream(featureProperties));
                    InputStream is = new BufferedInputStream(new FileInputStream(localFeatureProperties));
                    try {
                        IOUtil.copy(is, os);
                    } finally {
                        IOUtil.close(is);
                    }
                }

                if (licenseFeature != null) {
                    ZipFile zip = new ZipFile(licenseFeature);
                    try {
                        ZipEntry entry = zip.getEntry(FEATURE_PROPERTIES);
                        if (entry != null) {
                            if (os == null) {
                                os = new BufferedOutputStream(new FileOutputStream(featureProperties));
                            } else {
                                IOUtil.copy("\n", os);
                            }
                            InputStream is = zip.getInputStream(entry);
                            try {
                                IOUtil.copy(is, os);
                            } finally {
                                is.close();
                            }
                        }
                    } finally {
                        zip.close();
                    }
                } else if (localFeatureProperties.canRead()) {
                    featureProperties = localFeatureProperties;
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create feature.properties file for project " + project, e);
            } finally {
                if (os != null) {
                    IOUtil.close(os);
                }
            }
        }
        return featureProperties;
    }

    /**
     * @return A {@link FileSet} including files as configured by the <tt>bin.includes</tt> and
     *         <tt>bin.excludes</tt> properties without the files that are always included
     *         automatically.
     */
    private FileSet getManuallyIncludedFiles(BuildProperties buildProperties) {
        List<String> binExcludes = new ArrayList<>(buildProperties.getBinExcludes());
        binExcludes.add(Feature.FEATURE_XML); // we'll include updated feature.xml
        binExcludes.add(FEATURE_PROPERTIES); // we'll include updated feature.properties
        return getFileSet(basedir, buildProperties.getBinIncludes(), binExcludes);
    }

    private void assembleDeployableFeature() throws MojoExecutionException {
        UpdateSiteAssembler assembler = new UpdateSiteAssembler(session, target);
        getDependencyWalker().walk(assembler);
    }

    private void expandVersionQualifiers(Feature feature) throws MojoFailureException {
        feature.setVersion(DefaultReactorProject.adapt(project).getExpandedVersion());

        TargetPlatform targetPlatform = TychoProjectUtils.getTargetPlatformIfAvailable(project);
        if (targetPlatform == null) {
            getLog().warn(
                    "Skipping version reference expansion in eclipse-feature project using the deprecated -Dtycho.targetPlatform configuration");
            return;
        }
        featureXmlTransformer.expandReferences(feature, targetPlatform);
    }

    private JarArchiver getJarArchiver() throws MojoExecutionException {
        try {
            return (JarArchiver) plexus.lookup(Archiver.ROLE, "jar");
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Unable to get JarArchiver", e);
        }
    }
}
