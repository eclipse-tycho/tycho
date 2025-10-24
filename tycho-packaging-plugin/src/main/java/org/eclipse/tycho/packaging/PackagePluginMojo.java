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
 *    Christoph Läubrich 	- Issue #177 - Automatically translate maven-pom information to osgi Bundle-Header
 *    						- Issue #572 - Insert dynamic dependencies into the jar included pom
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.packaging.sourceref.SourceReferenceComputer;
import org.eclipse.tycho.packaging.sourceref.SourceReferencesProvider;
import org.osgi.framework.Constants;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 */
@Mojo(name = "package-plugin", threadSafe = true)
public class PackagePluginMojo extends AbstractTychoPackagingMojo {

	protected EclipsePluginProject pdeProject;

	/**
	 * The Jar archiver.
	 */
		@Inject
	private JarArchiver jarArchiver;

	/**
	 * Name of the generated JAR.
	 */
	@Parameter(property = "project.build.finalName", alias = "jarName", required = true)
	protected String finalName;

	/**
	 * The <a href="https://maven.apache.org/shared/maven-archiver/">maven
	 * archiver</a> to use. One of the archiver properties is the
	 * <code>addMavenDescriptor</code> flag, which indicates whether the generated
	 * archive will contain the pom.xml and pom.properties file. If no archive
	 * configuration is specified, the default value is <code>true</code>. If the
	 * maven descriptor should not be added to the artifact, use the following
	 * configuration:
	 *
	 * <pre>
	 * &lt;plugin&gt;
	 *   &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
	 *   &lt;artifactId&gt;tycho-packaging-plugin&lt;/artifactId&gt;
	 *   &lt;version&gt;${tycho-version}&lt;/version&gt;
	 *   &lt;configuration&gt;
	 *     &lt;archive&gt;
	 *       &lt;addMavenDescriptor&gt;false&lt;/addMavenDescriptor&gt;
	 *     &lt;/archive&gt;
	 *   &lt;/configuration&gt;
	 * &lt;/plugin&gt;
	 * </pre>
	 */
	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * Whether to generate an <a href=
	 * "https://wiki.eclipse.org/PDE/UI/SourceReferences">Eclipse-SourceReferences</a>
	 * MANIFEST header. When using this parameter, property ${tycho.scmUrl} must be
	 * set and be a valid
	 * <a href="https://maven.apache.org/scm/scm-url-format.html">maven SCM URL</a>.
	 *
	 * Example configuration:
	 *
	 * <pre>
	 *         &lt;sourceReferences&gt;
	 *           &lt;generate&gt;true&lt;/generate&gt;
	 *         &lt;/sourceReferences&gt;
	 * </pre>
	 *
	 * Note that a {@link SourceReferencesProvider} component must be registered for
	 * the SCM type being used. You may also override the generated value by
	 * configuring:
	 *
	 * <pre>
	 *         &lt;sourceReferences&gt;
	 *           &lt;generate&gt;true&lt;/generate&gt;
	 *           &lt;customValue&gt;scm:myscm:customSourceReferenceValue&lt;/customValue&gt;
	 *         &lt;/sourceReferences&gt;
	 * </pre>
	 */
	@Parameter
	private SourceReferences sourceReferences = new SourceReferences();

	/**
	 * Whether to derive OSGi-Headers from the maven-pom configuration, currently
	 * the following header are supported
	 * <ul>
	 * <li>{@link Constants#BUNDLE_LICENSE} is generated from maven &lt;licenses>
	 * configuration</li>
	 * </ul>
	 */
	@Parameter(defaultValue = "true")
	private boolean deriveHeaderFromProject = true;

	/**
	 * Whether to derive OSGi-Headers from the sources, currently the following
	 * header are supported
	 * <ul>
	 * <li>{@link Constants#REQUIRE_CAPABILITY} is enhanced by those from code
	 * annotations</li>
	 * </ul>
	 */
	@Parameter(defaultValue = "true")
	private boolean deriveHeaderFromSource;

	/**
	 * If {@code true}, it is checked that the explicitly declared OSGi service
	 * component files exist.
	 */
	@Parameter(defaultValue = "true")
	private boolean checkServiceComponentFilesExist = true;

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
	private SourceReferenceComputer soureReferenceComputer;

	@Inject
	TychoProjectManager projectManager;

	@Inject
	private BundleReader bundleReader;

	@Inject
	List<ManifestProcessor> manifestProcessors;

	@Inject
	PluginRealmHelper pluginRealmHelper;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip) {
			getLog().info("skip packaging");
			return;
		}
		Optional<EclipsePluginProject> pde = projectManager.getTychoProject(project)
				.filter(BundleProject.class::isInstance)
				.map(BundleProject.class::cast)
				.map(bundle -> bundle.getEclipsePluginProject(DefaultReactorProject.adapt(project)));
		pdeProject = pde.orElse(null);

		createSubJars();

		File pluginFile = createPluginJar();
		project.getArtifact().setFile(pluginFile);
	}

	private void createSubJars() throws MojoExecutionException {
		for (BuildOutputJar jar : pdeProject.getOutputJars()) {
			if (!jar.isDirClasspathEntry()) {
				makeJar(jar);
			}
		}
	}

	private File makeJar(BuildOutputJar jar) throws MojoExecutionException {
		String jarName = jar.getName();
		BuildProperties buildProperties = pdeProject.getBuildProperties();
		String customManifest = buildProperties.getJarToManifestMap().get(jarName);
		try {
			File jarFile = new File(project.getBasedir(), jarName);
			JarArchiver archiver = new JarArchiver();
			// configure for Reproducible Builds based on outputTimestamp value
			MavenArchiver.parseBuildOutputTimestamp(outputTimestamp).map(FileTime::from)
					.ifPresent(modifiedTime -> archiver.configureReproducibleBuild(modifiedTime));
			archiver.setDestFile(jarFile);
			File outputDirectory = jar.getOutputDirectory();
			if (!outputDirectory.mkdirs() && !outputDirectory.exists()) {
				throw new IOException("creating output directory " + outputDirectory.getAbsolutePath() + " failed");
			}
			archiver.addDirectory(outputDirectory);
			if (customManifest != null) {
				for (File sourceFolder : jar.getSourceFolders()) {
					File manifestFile = new File(sourceFolder, customManifest);
					if (manifestFile.isFile()) {
						archiver.setManifest(manifestFile);
						break;
					}
				}
			}
			archiver.createArchive();
			return jarFile;
		} catch (Exception e) {
			throw new MojoExecutionException("Could not create jar " + jarName, e);
		}
	}

	private File createPluginJar() throws MojoExecutionException {
		try {
			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);

			// configure for Reproducible Builds based on outputTimestamp value
			archiver.configureReproducibleBuild(outputTimestamp);

			File pluginFile = new File(buildDirectory, finalName + ".jar");
			if (pluginFile.exists()) {
				pluginFile.delete();
			}
			BuildProperties buildProperties = pdeProject.getBuildProperties();
			List<String> binIncludesList = buildProperties.getBinIncludes();
			List<String> binExcludesList = buildProperties.getBinExcludes();
			// 1. additional filesets should win over bin.includes, so we add them first
			if (additionalFileSets != null) {
				for (DefaultFileSet fileSet : additionalFileSets) {
					if (fileSet.getDirectory() != null && fileSet.getDirectory().isDirectory()) {
						archiver.getArchiver().addFileSet(fileSet);
					}
				}
			}
			List<String> binIncludesIgnoredForValidation = new ArrayList<>();
			// 2. handle dir classpath entries and "."
			for (BuildOutputJar outputJar : pdeProject.getOutputJarMap().values()) {
				String jarName = outputJar.getName();
				if (binIncludesList.contains(jarName) && outputJar.isDirClasspathEntry()) {
					binIncludesIgnoredForValidation.add(jarName);
					String prefix = ".".equals(jarName) ? "" : jarName;
					File outputDirectory = outputJar.getOutputDirectory();
					if (outputDirectory.isDirectory()) {
						archiver.getArchiver().addDirectory(outputDirectory, prefix);
					}
				}
			}
			// 3. handle nested jars and included resources
			checkBinIncludesExist(buildProperties, binIncludesIgnoredForValidation.toArray(new String[0]));
			// 4. check DS files exits...
			TychoProject facet = getTychoProjectFacet();
			if (checkServiceComponentFilesExist && facet instanceof OsgiBundleProject bundleProject) {
				String components = bundleProject.getManifestValue("Service-Component", project);
				if (components != null) {
					if (components.contains("*")) {
						getLog().warn(
								"Checking Service-Component header that contains wildcards is currently not supported");
					} else {
						for (String component : components.split(",\\s*")) {
							assertComponentExists(component);
						}
					}
				}
			}
			MavenProject mavenProject = project;
			archiver.getArchiver().addFileSet(getFileSet(mavenProject.getBasedir(), binIncludesList, binExcludesList));

			File manifest = new File(mavenProject.getBuild().getDirectory(), "MANIFEST.MF");
			writeManifest(manifest, getManifest());
			archive.setManifestFile(manifest);

			archiver.setOutputFile(pluginFile);
			if (!archive.isForced()) {
				// optimized archive creation not supported for now because of build qualifier
				// mismatch issues
				// see TYCHO-502
				getLog().warn("Ignoring unsupported archive 'forced = false' parameter");
				archive.setForced(true);
			}
			archiver.createArchive(session, mavenProject, archive);
			return pluginFile;
		} catch (IOException | ArchiverException | ManifestException | DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Error assembling JAR", e);
		}
	}

	private void assertComponentExists(String component) throws MojoExecutionException {
		File projectRelative = new File(project.getBasedir(), component);
		if (projectRelative.isFile()) {
			return;
		}
		File outputRelative = new File(project.getBuild().getOutputDirectory(), component);
		if (outputRelative.isFile()) {
			return;
		}
		String message = "Component definition " + component + " not found! Neither "
				+ projectRelative.getAbsolutePath() + " nor " + outputRelative.getAbsolutePath() + " exists.";
		if (strictBinIncludes) {
			throw new MojoExecutionException(message);
		}
		getLog().warn(message);

	}

	protected void writeManifest(File output, Manifest mf) throws IOException {
		File parentFile = output.getParentFile();
		if (!parentFile.mkdirs() && !parentFile.exists()) {
			throw new IOException("creating target directory " + parentFile.getAbsolutePath() + " failed");
		}
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(output))) {
			mf.write(os);
		}
	}

	protected Manifest getManifest() throws IOException, MojoExecutionException {
		// If the user explicitly set a MANIFEST file in the archive configuration,
		// read and update that one instead of looking under the source files
		final File archiveManifestFile = archive.getManifestFile();
		final File manifestFile = archiveManifestFile != null
				? archiveManifestFile
				: bundleReader.getManifestLocation(project);

		Manifest mf;
		try (final InputStream is = new FileInputStream(manifestFile)) {
			mf = new Manifest(is);
		}

		Attributes attributes = mf.getMainAttributes();

		if (attributes.getValue(Name.MANIFEST_VERSION) == null) {
			attributes.put(Name.MANIFEST_VERSION, "1.0");
		}
		ReactorProject reactorProject = DefaultReactorProject.adapt(project);
		attributes.putValue("Bundle-Version", reactorProject.getExpandedVersion());
		soureReferenceComputer.addSourceReferenceHeader(mf, sourceReferences, project);
		if (deriveHeaderFromProject) {
			computeIfHeaderNotPresent(attributes, Constants.BUNDLE_LICENSE, () -> {
				List<License> licenses = project.getLicenses();
				return licenses.stream().map(license -> {
					String name = license.getName();
					if (name != null && !name.isBlank()) {
						StringBuilder licenseHeader = new StringBuilder(name);
						appendHeaderAttribute(licenseHeader, "link", license.getUrl());
						return licenseHeader;
					}
					return null;
				}).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
			});
		}
		try {
			pluginRealmHelper.visitPluginExtensions(project, session, ManifestProcessor.class, processor -> {
				processor.processManifest(project, mf);
			});
		} catch (Exception e) {
			getLog().warn("Manifest processing failed: " + e);
		}

		return mf;
	}

	private static void appendHeaderAttribute(StringBuilder header, String attribute, String value) {
		if (value != null && !value.isBlank()) {
			header.append(";");
			header.append(attribute);
			header.append("=\"");
			header.append(value);
			header.append("\"");
		}
	}

	private static void computeIfHeaderNotPresent(Attributes attributes, String hv, Supplier<String> headerComputer) {
		if (attributes.getValue(hv) == null) {
			String header = headerComputer.get();
			if (header != null && !header.isBlank()) {
				attributes.putValue(hv, header);
			}
		}
	}

}
