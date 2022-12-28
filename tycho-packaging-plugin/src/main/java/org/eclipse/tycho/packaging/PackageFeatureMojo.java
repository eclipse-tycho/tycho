/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.IUDescription;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;
import org.eclipse.tycho.p2tools.RepositoryReferenceTool;

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
     * By default this is the Maven <tt>target/</tt> directory.
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

    /**
     * If set to <code>true</code>, standard eclipse update site directory with feature content will
     * be created under target folder.
     */
    @Parameter(defaultValue = "false")
    private boolean deployableFeature = false;

	/**
	 * The target repository name.
	 */
	@Parameter(defaultValue = "${project.name}")
	private String name;

	/**
	 * Follow only requirements which match the filter specified.
	 */
	@Parameter(defaultValue = "false")
	private boolean followOnlyFilteredRequirements;

	/**
	 * Set to <code>true</code> to filter the resulting set of IUs to only include
	 * the latest version of each Installable Unit only. By default, all versions
	 * satisfying dependencies are included.
	 */
	@Parameter(defaultValue = "false")
	private boolean latestVersionOnly;

	/**
	 * Whether to mirror metadata only (no artifacts).
	 */
	@Parameter(defaultValue = "false")
	private boolean mirrorMetadataOnly;

	/**
	 * Set to true if only strict dependencies should be followed. A strict
	 * dependency is defined by a version range only including exactly one version
	 * (e.g. [1.0.0.v2009, 1.0.0.v2009]). In particular, plugins/features included
	 * in a feature are normally required via a strict dependency from the feature
	 * to the included plugin/feature.
	 */
	@Parameter(defaultValue = "false")
	private boolean followStrictOnly;

	/**
	 * Whether or not to include features.
	 */
	@Parameter(defaultValue = "true")
	private boolean includeFeatures;

	/**
	 * Whether or not to follow optional requirements.
	 */
	@Parameter(defaultValue = "true")
	private boolean includeOptional;

	/**
	 * Whether or not to follow non-greedy requirements.
	 */
	@Parameter(defaultValue = "true")
	private boolean includeNonGreedy;

	/**
	 * <p>
	 * If set to true, mirroring continues to run in the event of an error during
	 * the mirroring process and will just log an info message.
	 * </p>
	 * 
	 * @since 1.1.0
	 */
	@Parameter(defaultValue = "false")
	private boolean ignoreErrors;

	/**
	 * Filter properties. In particular, a platform filter can be specified by using
	 * keys <code>osgi.os, osgi.ws, osgi.arch</code>.
	 */
	@Parameter
	private Map<String, String> filter = new HashMap<>();

	/**
	 * Whether to compress the destination repository metadata files (artifacts.xml,
	 * content.xml).
	 */
	@Parameter(defaultValue = "true")
	private boolean compress;

	/**
	 * Whether to append to an existing destination repository. Note that appending
	 * an IU which already exists in the destination repository will cause the
	 * mirror operation to fail.
	 */
	@Parameter(defaultValue = "true")
	private boolean append;

	/**
	 * <p>
	 * Add XZ-compressed repository index files. XZ offers better compression ratios
	 * esp. for highly redundant file content.
	 * </p>
	 * 
	 * @since 0.25.0
	 */
	@Parameter(defaultValue = "true")
	private boolean xzCompress;

	/**
	 * <p>
	 * If {@link #xzCompress} is <code>true</code>, whether jar or xml index files
	 * should be kept in addition to XZ-compressed index files. This fallback
	 * provides backwards compatibility for pre-Mars p2 clients which cannot read
	 * XZ-compressed index files.
	 * </p>
	 * 
	 * @since 0.25.0
	 */
	@Parameter(defaultValue = "true")
	private boolean keepNonXzIndexFiles;

    @Parameter(defaultValue = "${project.build.directory}/site")
    private File target;

    @Component
    private FeatureXmlTransformer featureXmlTransformer;

    @Component
    private LicenseFeatureHelper licenseFeatureHelper;

	@Component
	private TargetPlatformService platformService;

	@Component
	private BuildPropertiesParser buildPropertiesParser;

	@Component
	private RepositoryReferenceTool repositoryReferenceTool;

	@Component
	private MirrorApplicationService mirrorService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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

            if (deployableFeature) {
				assembleDeployableFeature(feature);
            }
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
     * @return A {@link FileSet} including files as configured by the <tt>bin.includes</tt> and
     *         <tt>bin.excludes</tt> properties without the files that are always included
     *         automatically.
     */
    private FileSet getManuallyIncludedFiles(BuildProperties buildProperties) {
        List<String> binExcludes = new ArrayList<>(buildProperties.getBinExcludes());
        binExcludes.add(FEATURE_XML); // we'll include updated feature.xml
        binExcludes.add(FEATURE_PROPERTIES); // we'll include updated feature.properties
        return getFileSet(basedir, buildProperties.getBinIncludes(), binExcludes);
    }

	private void assembleDeployableFeature(Feature feature) throws MojoExecutionException, MojoFailureException {
		RepositoryReferences sourceDescriptor = repositoryReferenceTool.getVisibleRepositories(this.project,
				this.session, RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE);
		final DestinationRepositoryDescriptor destinationDescriptor = new DestinationRepositoryDescriptor(target, name,
				compress, xzCompress, keepNonXzIndexFiles, mirrorMetadataOnly, append, Collections.emptyMap(),
				Collections.emptyList());
		try {
			mirrorService.mirrorStandalone(sourceDescriptor, destinationDescriptor,
					List.of(new IUDescription(feature.getId(), feature.getVersion())),
					createMirrorOptions(), getBuildOutputDirectory());
		} catch (final FacadeException e) {
			throw new MojoExecutionException("Error during mirroring", e);
		}
    }

	private MirrorOptions createMirrorOptions() {
		MirrorOptions options = new MirrorOptions();
		options.setFollowOnlyFilteredRequirements(followOnlyFilteredRequirements);
		options.setFollowStrictOnly(followStrictOnly);
		options.setIncludeFeatures(includeFeatures);
		options.setIncludeNonGreedy(includeNonGreedy);
		options.setIncludeOptional(includeOptional);
		options.setLatestVersionOnly(latestVersionOnly);
		options.getFilter().putAll(filter);
		options.setIgnoreErrors(ignoreErrors);
		return options;
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

	private BuildDirectory getBuildOutputDirectory() {
		return DefaultReactorProject.adapt(project).getBuildDirectory();
	}

}
