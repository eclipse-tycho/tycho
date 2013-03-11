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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;

/**
 * @phase package
 * @goal package-feature
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class PackageFeatureMojo extends AbstractTychoPackagingMojo {

    private static final String FEATURE_PROPERTIES = "feature.properties";

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

    /**
     * @component
     */
    private BuildPropertiesParser buildPropertiesParser;

    public void execute() throws MojoExecutionException, MojoFailureException {
        expandVersion();
        outputDirectory.mkdirs();

        Feature feature = Feature.loadFeature(basedir);

        File licenseFeature = getLicenseFeature(feature);

        // remove license feature id and version from feature.xml
        feature.setLicenseFeature(null);
        feature.setLicenseFeatureVersion(null);

        File featureXml = new File(outputDirectory, Feature.FEATURE_XML);
        try {
            feature = expandVersionQualifiers(feature);
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
                archiver.getArchiver().addArchivedFileSet(getLicenseFeatureFileSet(licenseFeature));
            }
            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);
            if (featureProperties != null) {
                archiver.getArchiver().addFile(featureProperties, FEATURE_PROPERTIES);
            }
            archiver.createArchive(project, archive);
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating feature package", e);
        }

        project.getArtifact().setFile(outputJar);

        if (deployableFeature) {
            assembleDeployableFeature(feature);
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
        List<String> binExcludes = new ArrayList<String>(buildProperties.getBinExcludes());
        binExcludes.add(Feature.FEATURE_XML); // we'll include updated feature.xml
        binExcludes.add(FEATURE_PROPERTIES); // we'll include updated feature.properties
        return getFileSet(basedir, buildProperties.getBinIncludes(), binExcludes);
    }

    private ArchivedFileSet getLicenseFeatureFileSet(File licenseFeature) throws IOException {
        // copy all files from license feature's build.properties file except feature.properties and feature.xml

        BuildProperties buildProperties;

        ZipFile zip = new ZipFile(licenseFeature);
        try {
            ZipEntry entry = zip.getEntry(BuildPropertiesParser.BUILD_PROPERTIES);
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                Properties p = new Properties();
                p.load(is);
                buildProperties = new BuildPropertiesImpl(p);
            } else {
                throw new IllegalArgumentException("license feature must include build.properties file");
            }
        } finally {
            zip.close();
        }

        List<String> includes = buildProperties.getBinIncludes();

        Set<String> excludes = new HashSet<String>(buildProperties.getBinExcludes());
        excludes.add(Feature.FEATURE_XML);
        excludes.add(FEATURE_PROPERTIES);
        excludes.add(BuildPropertiesParser.BUILD_PROPERTIES);

        // mavenArchiver ignores license feature files that are also present in 'this' feature
        // i.e. if there is a conflict, files from 'this' feature win

        DefaultArchivedFileSet result = new DefaultArchivedFileSet();
        result.setArchive(licenseFeature);
        result.setIncludes(includes.toArray(new String[includes.size()]));
        result.setExcludes(excludes.toArray(new String[excludes.size()]));

        return result;
    }

    /**
     * Process license feature as described in http://wiki.eclipse.org/Equinox/p2/License_Mechanism
     */
    private File getLicenseFeature(Feature feature) {
        String id = feature.getLicenseFeature();

        if (id == null) {
            return null;
        }

        ArtifactDescriptor licenseFeature = getDependencyArtifacts().getArtifact(ArtifactKey.TYPE_ECLIPSE_FEATURE, id,
                feature.getLicenseFeatureVersion());

        if (licenseFeature == null) {
            throw new IllegalStateException("License feature with id " + id
                    + " is not found among project dependencies");
        }

        ReactorProject licenseProject = licenseFeature.getMavenProject();
        if (licenseProject == null) {
            return licenseFeature.getLocation();
        }

        File artifact = licenseProject.getArtifact();
        if (!artifact.isFile()) {
            throw new IllegalStateException("At least ``package'' phase need to be executed");
        }

        return artifact;
    }

    private void assembleDeployableFeature(Feature feature) throws MojoExecutionException {
        UpdateSiteAssembler assembler = new UpdateSiteAssembler(session, target);
        getDependencyWalker().walk(assembler);
    }

    private Feature expandVersionQualifiers(Feature feature) throws MojoExecutionException, IOException {
        return featureXmlTransformer.transform(DefaultReactorProject.adapt(project), feature, getDependencyWalker());
    }

    private JarArchiver getJarArchiver() throws MojoExecutionException {
        try {
            return (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Unable to get JarArchiver", e);
        }
    }
}
