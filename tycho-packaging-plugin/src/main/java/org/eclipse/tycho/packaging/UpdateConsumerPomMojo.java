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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.packaging.reverseresolve.ArtifactCoordinateResolver;

/**
 * Updates the pom file with the dependencies from the tycho model. If you
 * further like to customize the pom you should take a look at the <a href=
 * "https://www.mojohaus.org/flatten-maven-plugin/plugin-info.html">Maven
 * Flatten Plugin</a>
 */
@Mojo(name = "update-consumer-pom", threadSafe = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class UpdateConsumerPomMojo extends AbstractMojo {

	private static final String POLYGLOT_POM_TYCHO = ".polyglot.pom.tycho";

	@Parameter(property = "project", readonly = true)
	protected MavenProject project;

	@Component(role = ModelWriter.class)
	protected ModelWriter modelWriter;

	@Component(role = ModelReader.class)
	protected ModelReader modelReader;

	@Component
	private Map<String, ArtifactCoordinateResolver> artifactCoordinateResolvers;

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

	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	@Parameter(defaultValue = "local,central")
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
		getLog().debug("Generate pom descriptor with updated dependencies...");
		Model projectModel;
		try {
			projectModel = modelReader.read(project.getFile(), null);
		} catch (IOException e) {
			throw new MojoExecutionException("reading the model failed!", e);
		}
		List<Dependency> dependencies = projectModel.getDependencies();
		dependencies.clear();
		List<Dependency> list = Objects.requireNonNullElse(project.getDependencies(), Collections.emptyList());
		for (Dependency dep : list) {
			Dependency copy = dep.clone();
			if (Artifact.SCOPE_SYSTEM.equals(dep.getScope())) {
				if (!handleSystemScopeDependency(copy)) {
					// skip this ...
					continue;
				}
			}
			dependencies.add(copy);
		}
		Parent parent = projectModel.getParent();
		if (parent != null) {
			String relativePath = parent.getRelativePath();
			if (relativePath != null && relativePath.endsWith(POLYGLOT_POM_TYCHO)) {
				parent.setRelativePath(
						relativePath.substring(0, relativePath.length() - POLYGLOT_POM_TYCHO.length()) + "pom.xml");
			}
		}
		File output = new File(outputDirectory, tychoPomFilename);
		if (deleteOnExit) {
			output.deleteOnExit();
		}
		try {
			modelWriter.write(output, null, projectModel);
		} catch (IOException e) {
			throw new MojoExecutionException("writing the model failed!", e);
		}
		if (updatePomFile) {
			project.setFile(output);
		}

	}

	private boolean handleSystemScopeDependency(Dependency dep) {
		if (dep.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX) || isEmbeddedJar(dep)) {
			if (mapP2Dependencies && !isEmbeddedJar(dep)) {
				Path p = getPath(dep);
				Optional<Dependency> resolved = resolvedDependencies.computeIfAbsent(p.normalize().toString(), nil -> {
					return Arrays.stream(resolver.split(",")).map(String::strip).map(artifactCoordinateResolvers::get)
							.filter(Objects::nonNull).flatMap(resolver -> resolver.resolve(p).stream()).findFirst();
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
			dep.setOptional(true);
			dep.setScope(Artifact.SCOPE_PROVIDED);
			dep.setSystemPath(null);
			return includeP2Dependencies;
		}
		return true;
	}

	private Path getPath(Dependency dep) {
		String systemPath = dep.getSystemPath();
		if (systemPath != null) {
			try {
				return Path.of(systemPath).toAbsolutePath();
			} catch (InvalidPathException e) {
				// then we can't use it ...
			}
		}
		return null;
	}

	private boolean isEmbeddedJar(Dependency dep) {
		return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(dep.getType()) && dep.getClassifier() != null
				&& !dep.getClassifier().isBlank();
	}

}
