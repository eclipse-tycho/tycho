/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.ResolutionScope;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.packaging.reverseresolve.ArtifactCoordinateResolver;

/**
 * Updates the pom file with the dependencies from the tycho model. If you
 * further like to customize the pom you should take a look at the <a href=
 * "https://www.mojohaus.org/flatten-maven-plugin/plugin-info.html">Maven
 * Flatten Plugin</a>
 * <p>
 * See also <a href=
 * "https://cwiki.apache.org/confluence/display/MAVEN/Build+vs+Consumer+POM">Build
 * vs Consumer POM</a>
 * </p>
 */
@Mojo(name = "update-consumer-pom", threadSafe = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class UpdateConsumerPomMojo extends AbstractMojo {

	private static final String POLYGLOT_POM_TYCHO = ".polyglot.pom.tycho";

	@Parameter(property = "project", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

		@Inject
	protected ModelWriter modelWriter;

		@Inject
	protected ModelReader modelReader;

	@Inject
	private Map<String, ArtifactCoordinateResolver> artifactCoordinateResolvers;

	@Inject
	ArtifactHandlerManager artifactHandlerManager;

	/**
	 * The directory where the tycho generated POM file will be written to.
	 */
	@Parameter(defaultValue = "${project.basedir}", required = true)
	protected File outputDirectory;

	/**
	 * The filename of the tycho generated POM file.
	 */
	@Parameter(defaultValue = ".tycho-consumer-pom.xml", required = true)
	protected String tychoPomFilename;

	/**
	 * If deleteOnExit is true the file will be marked for deletion on JVM exit
	 */
	@Parameter(defaultValue = "true")
	protected boolean deleteOnExit = true;

	/**
	 * replace they type of a dependency (e.g. 'eclipse-plugin') with its extension
	 * (e.g. 'jar')
	 */
	@Parameter(defaultValue = "true")
	protected boolean replaceTypeWithExtension = true;

	@Parameter
	protected Boolean skipPomGeneration;

	/**
	 * Indicate if the generated tycho POM should become the new project.
	 */
	@Parameter(defaultValue = "true")
	protected boolean updatePomFile = true;

	/**
	 * If includeP2Dependencies is true Tycho will include P2 dependencies in the
	 * consumer pom as optional items with provided scope. If the value is false,
	 * such items are skipped and only such items are included that can be mapped to
	 * a valid maven artifact
	 */
	@Parameter(defaultValue = "false")
	protected boolean includeP2Dependencies = false;

	/**
	 * If mapP2Dependencies is true then Tycho tries to reverse-resolve P2
	 * dependencies to true maven artifact coordinates and include those in the pom
	 * instead of the P2 system scoped ones.
	 */
	@Parameter(defaultValue = "false")
	protected boolean mapP2Dependencies = false;

	/**
	 * if enabled, replace the real packaging-type (e.g.
	 * <code>eclipse-plugin</code>) with its implied one (e.g. <code>jar</code>).
	 * This makes the pom look more like a "plain" one and ensure that such
	 * dependencies are not confused by other tools (including maven).
	 */
	@Parameter(defaultValue = "true")
	protected boolean replacePackagingType = true;

	/**
	 * If enabled, modules are removed from the consumer pom
	 */
	@Parameter(defaultValue = "true")
	protected boolean removeModules = true;

	/**
	 * If enabled, build section is removed from the consumer pom
	 */
	@Parameter(defaultValue = "true")
	protected boolean removeBuild = true;

	/**
	 * If enabled, removes the relative path from the parent
	 */
	@Parameter(defaultValue = "true")
	protected boolean relativePathFromParent = true;

	/**
	 * If enabled, removes the parent and resolves version and group id directly
	 */
	@Parameter(defaultValue = "true")
	protected boolean removeParent = true;

	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	@Parameter(defaultValue = "local,p2,central")
	private String resolver;

	private Map<String, Optional<Dependency>> resolvedDependencies = new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipPomGeneration == null) {
			if (!archive.isAddMavenDescriptor()) {
				return;
			}
		} else if (skipPomGeneration) {
			return;
		}
		if (outputDirectory == null) {
			outputDirectory = project.getBasedir();
		}
		Log log = getLog();
		log.debug("Generating pom descriptor with updated dependencies");
		Model projectModel;
		try {
			projectModel = modelReader.read(project.getFile(), Map.of());
		} catch (IOException e) {
			throw new MojoExecutionException("reading the model failed!", e);
		}
		// just in case this is a CI-Friendly version replace it with actual value
		projectModel.setVersion(project.getVersion());
		if (removeParent) {
			projectModel.setGroupId(project.getGroupId());
			projectModel.setParent(null);
		}
		List<Dependency> dependencies = projectModel.getDependencies();
		dependencies.clear();
		List<Dependency> list = Objects.requireNonNullElse(project.getDependencies(), Collections.emptyList());
		Set<String> p2Skipped = new TreeSet<>();
		int resolved = 0;
		for (Dependency dep : list) {
			Dependency copy = dep.clone();
			if (Artifact.SCOPE_SYSTEM.equals(dep.getScope())) {
				if (!handleSystemScopeDependency(copy)) {
					p2Skipped.add(dep.getManagementKey() + " @ "
							+ ArtifactCoordinateResolver.getPath(dep).map(String::valueOf).orElse(dep.getSystemPath()));
					// skip this ...
					continue;
				}
				resolved++;
			}
			if (replaceTypeWithExtension && PackagingType.TYCHO_PACKAGING_TYPES.contains(copy.getType())) {
				ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(copy.getType());
				if (handler != null) {
					copy.setType(handler.getExtension());
				}
			}
			dependencies.add(copy);
		}
		Parent parent = projectModel.getParent();
		if (parent != null) {
			MavenProject parentmavenProject = project.getParent();
			if (parentmavenProject != null) {
				// just in case this is a CI-Friendly version replace it with actual value
				parent.setVersion(parentmavenProject.getVersion());
			}
			if (relativePathFromParent) {
				parent.setRelativePath("../pom.xml"); // will be treated as default and then removed...
			} else {
				String relativePath = parent.getRelativePath();
				if (relativePath != null && relativePath.endsWith(POLYGLOT_POM_TYCHO)) {
					parent.setRelativePath(
							relativePath.substring(0, relativePath.length() - POLYGLOT_POM_TYCHO.length()) + "pom.xml");
				}
			}
		}
		if (replacePackagingType) {
			projectModel.setPackaging(mapTychoPackagingTypeToMaven(projectModel.getPackaging()));
		}
		if (removeModules) {
			projectModel.setModules(null);
		}
		if (removeBuild) {
			projectModel.setBuild(null);
		}
		File output = new File(outputDirectory, tychoPomFilename);
		if (deleteOnExit) {
			output.deleteOnExit();
		}
		if (p2Skipped.isEmpty()) {
			log.info("All system scoped dependencies were mapped to maven artifacts");
		} else if (mapP2Dependencies) {
			log.warn(resolved + " system scoped dependencies were mapped to maven artifacts, " + p2Skipped.size()
					+ " were skipped");
			if (log.isDebugEnabled()) {
				for (String skipped : p2Skipped) {
					log.debug("Skipped: " + skipped);
				}
			}
		} else {
			log.info(p2Skipped.size() + " system scoped dependencies were not mapped to maven artifacts");
		}
		try {
			modelWriter.write(output, null, projectModel);
		} catch (IOException e) {
			throw new MojoExecutionException("writing the model failed!", e);
		}
		try {
			if (p2Skipped.size() > 0) {
				File file = new File(project.getBuild().getDirectory(), "skippedP2Dependencies.txt");
				file.getParentFile().mkdirs();
				Files.writeString(file.toPath(), p2Skipped.stream().collect(Collectors.joining("\r\n")));
			}
		} catch (IOException e) {
			log.warn("Writing additional information failed: " + e);
		}
		if (updatePomFile) {
			project.setFile(output);
		}

	}

	/**
	 * This maps tycho-types to "maven" types to be used in a plain maven build
	 * without Tycho used at all. Because in such case the mappings are missing
	 * Maven try to use <code>packaging</code> ==
	 * <code>&lt;file extension&gt;</code> as a default.
	 * 
	 * @param packaging
	 * @return
	 */
	private String mapTychoPackagingTypeToMaven(String packaging) {
		if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
				|| PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
			return "jar";
		}
		if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
			return "pom";
		}
		if (PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(packaging)) {
			return "target";
		}
		if (PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(packaging)) {
			return "zip";
		}
		return packaging;
	}

	private boolean handleSystemScopeDependency(Dependency dep) {
		if (dep.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX) || isEmbeddedJar(dep)) {
			if (mapP2Dependencies && !isEmbeddedJar(dep)) {
				Optional<Path> path = ArtifactCoordinateResolver.getPath(dep);
				if (path.isPresent()) {
					Optional<Dependency> resolved = resolvedDependencies
							.computeIfAbsent(path.get().normalize().toString(), nil -> {
								return Arrays.stream(resolver.split(",")).map(String::strip)
										.map(artifactCoordinateResolvers::get).filter(Objects::nonNull)
										.flatMap(resolver -> resolver.resolve(dep, project, session).stream())
										.findFirst();
							});
					if (resolved.isPresent()) {
						dep.setScope(Artifact.SCOPE_COMPILE); // TODO what about test dependencies?
						dep.setSystemPath(null);
						Dependency dependency = resolved.get();
						dep.setArtifactId(dependency.getArtifactId());
						dep.setGroupId(dependency.getGroupId());
						dep.setVersion(dependency.getVersion());
						dep.setType(dependency.getType());
						return true;
					}
				}
			}
			dep.setOptional(true);
			dep.setScope(Artifact.SCOPE_PROVIDED);
			dep.setSystemPath(null);
			return includeP2Dependencies;
		}
		return true;
	}

	private boolean isEmbeddedJar(Dependency dep) {
		return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(dep.getType()) && dep.getClassifier() != null
				&& !dep.getClassifier().isBlank();
	}

}
