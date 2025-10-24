/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #519941 Copy the shared license info
 *    Christoph Läubrich - Issue #572 - Insert dynamic dependencies into the jar included pom 
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import static org.eclipse.tycho.model.Feature.FEATURE_XML;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.ResolutionScope;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;

@Mojo(name = "package-feature", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class PackageFeatureMojo extends AbstractTychoPackagingMojo {
    private static final Object LOCK = new Object();

    private static final String FEATURE_PROPERTIES = "feature.properties";

    /**
     * The <a href="https://maven.apache.org/shared/maven-archiver/">maven archiver</a> to use. One
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
     * By default this is the Maven <code>target/</code> directory.
     */
    @Parameter(property = "project.build.directory")
    private File outputDirectory;

    @Parameter(property = "project.basedir")
    private File basedir;

    /**
     * The path to the <code>feature.xml</code> file.
     * <p>
     * Defaults to the <code>feature.xml</code> under the project's base directory.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FEATURE_XML)
    private File featureFile;

    /**
     * Name of the generated JAR.
     */
    @Parameter(property = "project.build.finalName", alias = "jarName", required = true)
    private String finalName;

    @Parameter(defaultValue = "${project.build.directory}/site")
    private File target;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO
     * 8601 extended offset date-time (e.g. in UTC such as '2011-12-03T10:15:30Z' or
     * with an offset '2019-10-05T20:37:42+06:00'), or as an int representing
     * seconds since the epoch (like <a href=
     * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    @Inject
    private FeatureXmlTransformer featureXmlTransformer;

    @Inject
    private LicenseFeatureHelper licenseFeatureHelper;

	@Inject
	private TargetPlatformService platformService;

	@Inject
	private BuildPropertiesParser buildPropertiesParser;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skip packaging");
            return;
        }
        synchronized (LOCK) {
            outputDirectory.mkdirs();

            if (!featureFile.isFile()) {
                throw new MojoExecutionException("The featureFile parameter must represent a valid file");
            }

            Feature feature;

            try {
                feature = Feature.read(featureFile);
            } catch (final IOException e) {
                throw new MojoExecutionException("Error reading " + featureFile, e);
            }

            File licenseFeature = licenseFeatureHelper.getLicenseFeature(feature, project);

            updateLicenseProperties(feature, licenseFeature);

            File featureXml = new File(outputDirectory, FEATURE_XML);
            try {
                expandVersionQualifiers(feature);
                Feature.write(feature, featureXml);
            } catch (IOException e) {
                throw new MojoExecutionException("Error updating feature.xml", e);
            }

			BuildProperties buildProperties = buildPropertiesParser.parse(DefaultReactorProject.adapt(project));
            checkBinIncludesExist(buildProperties);

            File featureProperties = getFeatureProperties(licenseFeature, buildProperties);

            File outputJar = new File(outputDirectory, finalName + ".jar");
            outputJar.getParentFile().mkdirs();

            MavenArchiver archiver = new MavenArchiver();
            JarArchiver jarArchiver = getJarArchiver();
            archiver.setArchiver(jarArchiver);
            // configure for Reproducible Builds based on outputTimestamp value
            archiver.configureReproducibleBuild(outputTimestamp);
            archiver.setOutputFile(outputJar);
            jarArchiver.setDestFile(outputJar);

            try {
                // Additional file sets win over bin.includes ones, so we add them first
                if (additionalFileSets != null) {
                    for (final var fileSet : additionalFileSets) {
                        final var directory = fileSet.getDirectory();

                        // noinspection ConstantConditions
                        if (directory != null && directory.isDirectory()) {
                            archiver.getArchiver().addFileSet(fileSet);
                        }
                    }
                }

                archiver.getArchiver().addFileSet(getManuallyIncludedFiles(buildProperties));
                if (licenseFeature != null) {
                    archiver.getArchiver()
                            .addArchivedFileSet(licenseFeatureHelper.getLicenseFeatureFileSet(licenseFeature));
                }
                archiver.getArchiver().addFile(featureXml, FEATURE_XML);
                if (featureProperties != null) {
                    archiver.getArchiver().addFile(featureProperties, FEATURE_PROPERTIES);
                }
                if (archive == null) {
                    archive = new MavenArchiveConfiguration();
                    archive.setAddMavenDescriptor(false);
                }
				MavenProject mavenProject = project;
				archiver.createArchive(session, mavenProject, archive);
            } catch (Exception e) {
                throw new MojoExecutionException("Error creating feature package", e);
            }

            project.getArtifact().setFile(outputJar);
        }
    }

    private void updateLicenseProperties(Feature feature, File licenseFeatureFile) {
        // remove license feature id and version from feature.xml
        feature.setLicenseFeature(null);
        feature.setLicenseFeatureVersion(null);
        // copy the license text and URL from the license feature
        if (licenseFeatureFile != null) {
            Feature licenseFeature = Feature.loadFeature(licenseFeatureFile);
            if (licenseFeature.getLicenseURL() != null) {
                feature.setLicenseURL(licenseFeature.getLicenseURL());
            }
            if (licenseFeature.getLicense() != null) {
                feature.setLicense(licenseFeature.getLicense());
            }
        }
    }

    private File getFeatureProperties(File licenseFeature, BuildProperties buildProperties)
            throws MojoExecutionException {
        try {
            File localFeatureProperties = new File(basedir, FEATURE_PROPERTIES);
            File targetFeatureProperties = new File(outputDirectory, FEATURE_PROPERTIES);
            if (targetFeatureProperties.exists() && !targetFeatureProperties.delete()) {
                throw new MojoExecutionException("Could not delete file " + targetFeatureProperties.getAbsolutePath());
            }
            // copy the feature.properties from the current feature to the target directory
            if (buildProperties.getBinIncludes().contains(FEATURE_PROPERTIES) && localFeatureProperties.canRead()) {
                Files.copy(localFeatureProperties.toPath(), targetFeatureProperties.toPath());
            }
            // if there is a license feature, append to the existing feature.properties or create
            // a new one containing the license features's feature.properties content
            if (licenseFeature != null) {
                appendToOrAddFeatureProperties(targetFeatureProperties, licenseFeature);
            }
            if (targetFeatureProperties.exists()) {
                return targetFeatureProperties;
            }
            return null;
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create feature.properties file for project " + project, e);
        }
    }

    private void appendToOrAddFeatureProperties(File targetFeatureProperties, File licenseFeature) throws IOException {
        try (ZipFile zip = new ZipFile(licenseFeature)) {
            ZipEntry entry = zip.getEntry(FEATURE_PROPERTIES);
            if (entry != null) {
                try (InputStream inputStream = zip.getInputStream(entry);
                        FileWriter writer = new FileWriter(targetFeatureProperties.getAbsolutePath(), true)) {
                    // if we append, first add a new line to be sure that we start 
                    // in a new line of the existing file
                    if (targetFeatureProperties.exists()) {
                        IOUtil.copy("\n", writer);
                    }
                    IOUtil.copy(inputStream, writer);
                }
            }
        }
    }

    /**
     * @return A {@link FileSet} including files as configured by the <code>bin.includes</code> and
     *         <code>bin.excludes</code> properties without the files that are always included
     *         automatically.
     */
    private FileSet getManuallyIncludedFiles(BuildProperties buildProperties) {
        List<String> binExcludes = new ArrayList<>(buildProperties.getBinExcludes());
        binExcludes.add(FEATURE_XML); // we'll include updated feature.xml
        binExcludes.add(FEATURE_PROPERTIES); // we'll include updated feature.properties
        return getFileSet(basedir, buildProperties.getBinIncludes(), binExcludes);
    }

    private void expandVersionQualifiers(Feature feature) throws MojoFailureException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        feature.setVersion(reactorProject.getExpandedVersion());

		TargetPlatform targetPlatform = platformService.getTargetPlatform(reactorProject).orElse(null);
		if (targetPlatform == null) {
			getLog().warn(
                    "Skipping version reference expansion in eclipse-feature project using the deprecated -Dtycho.targetPlatform configuration");
			return;
		}
		featureXmlTransformer.expandReferences(feature, targetPlatform);
    }

    private JarArchiver getJarArchiver() throws MojoExecutionException {
        try {
			return plexus.lookup(JarArchiver.class, "jar");
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Unable to get JarArchiver", e);
        }
    }
}
