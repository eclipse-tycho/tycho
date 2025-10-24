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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.packaging.osgiresolve.OSGiResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * This mojo verifies the pom of the project that is can be resolved against
 * your build repositories and that all resolved dependencies can be used inside
 * OSGi without any missing requirements.
 */
@Mojo(name = VerifyPomMojo.NAME, threadSafe = true)
public class VerifyPomMojo extends AbstractMojo {
	static final String NAME = "verify-osgi-pom";

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "tycho.verify.pom")
	private boolean skip;

	@Parameter(property = "tycho.verify.failOnError")
	private boolean failOnError;

	@Inject
	private RepositorySystem repositorySystem;

	@Inject
	private ModelReader modelReader;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Execution was skipped");
			return;
		}
		File pom;
		Log log = getLog();
		boolean isPlugin = PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging());
		boolean isFeature = PackagingType.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging());
		if (isPlugin || isFeature) {
			pom = project.getFile();
		} else {
			log.info("Skipped project because of incompatible packaging type");
			return;
		}
		try {
			Model model = modelReader.read(pom,
					Map.of(ModelReader.IS_STRICT, true, ModelReader.INPUT_SOURCE, new InputSource()));
			ArtifactTypeRegistry typeRegistry = session.getRepositorySession().getArtifactTypeRegistry();
			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRepositories(project.getRemoteProjectRepositories());
			Map<String, InputLocation> locationMap = new HashMap<>();
			for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
				collectRequest.addDependency(RepositoryUtils.toDependency(dependency, typeRegistry));
				locationMap.put(getId(dependency), dependency.getLocation(""));
			}
			DependencyRequest dependencyRequest = new DependencyRequest();
			dependencyRequest.setCollectRequest(collectRequest);
			DependencyResult dependencyResult = repositorySystem.resolveDependencies(session.getRepositorySession(),
					dependencyRequest);
			Set<TrackedDependency> filesToResolve = new HashSet<>();
			if (isPlugin) {
				// for plugins we need to add the artifact itself ...
				Artifact artifact = project.getArtifact();
				InputLocation location = model.getLocation("artifactId");
				filesToResolve.add(new TrackedDependency(project.getId(), location, artifact.getFile()));
			}
			for (ArtifactResult resolved : dependencyResult.getArtifactResults()) {
				org.eclipse.aether.artifact.Artifact artifact = resolved.getArtifact();
				if (artifact != null) {
					String id = getId(artifact);
					InputLocation location = locationMap.get(id);
					File file = artifact.getFile();
					TrackedDependency dependency = new TrackedDependency(id, location, file);
					if (file != null) {
						filesToResolve.add(dependency);
					} else {
						logError(dependency, "Failed to resolve artifact file", pom);
					}
				} else {
					// failed... could it happen? Should we not get DependencyResolutionException?
					logError(new TrackedDependency(project.getId(), null, pom),
							"Failed to resolve dependencies: " + resolved.getExceptions(), pom);
				}
			}
			if (filesToResolve.isEmpty()) {
				// nothing to do...
				return;
			}
			// now use the data to check the resolve...
			OSGiResolver resolver = new OSGiResolver(
					new File(project.getBuild().getDirectory(), UUID.randomUUID().toString()));
			Map<Bundle, TrackedDependency> bundleMap = new HashMap<>();
			log.info("Using " + filesToResolve.size() + " artifacts to check OSGi consistency");
			for (TrackedDependency tracked : filesToResolve) {
				try {
					Bundle install = resolver.install(tracked.file);
					if (install != null) {
						log.debug("Installed " + tracked.id + " as " + install.getSymbolicName());
						bundleMap.put(install, tracked);
					} else {
						// might not be an OSGi bundle... but maybe not an issue...
						log.debug("Cannot install resolved dependency " + tracked.id + " ("
								+ tracked.file.getAbsolutePath()
								+ ") for verification inside OSGi, maven compilation might still work");
					}
				} catch (BundleException e) {
					logError(tracked, e.getMessage(), pom);
				}
			}
			Map<Bundle, String> resolveErrors = resolver.resolve();
			if (resolveErrors.isEmpty()) {
				log.info("No consistency errors found");
			} else {
				Iterator<Entry<Bundle, String>> iterator = resolveErrors.entrySet().stream()
						.sorted(Comparator.comparing(Entry::getKey,
								Comparator.comparing(Bundle::getSymbolicName, String.CASE_INSENSITIVE_ORDER)))
						.iterator();
				while (iterator.hasNext()) {
					Map.Entry<org.osgi.framework.Bundle, java.lang.String> entry = iterator.next();
					Bundle bundle = entry.getKey();
					TrackedDependency trackedDependency = bundleMap.get(bundle);
					String message = entry.getValue();
					logError(trackedDependency, message, pom);
				}
			}
		} catch (ModelParseException e) {
			logError(new TrackedDependency(project.getId(), null, pom), "Failed to parse pom model: " + e.getMessage(),
					pom);
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		} catch (DependencyResolutionException e) {
			logError(new TrackedDependency(project.getId(), null, pom),
					"Failed to resolve pom dependencies: " + e.getMessage(), pom);
		} catch (BundleException e) {
			throw new MojoFailureException("can't start the framework", e);
		}
	}

	private void logError(TrackedDependency trackedDependency, String message, File pom) throws MojoFailureException {
		if (trackedDependency != null) {
			String oneLineMsg = failOnError ? message : message.replaceAll("[\\r\\n]+", " ");
			InputLocation location = trackedDependency.location;
			String content = "[" + NAME + "] " + trackedDependency.id + ": " + oneLineMsg + " @ file: "
					+ pom.getAbsolutePath();
			if (location != null) {
				content += ", line: " + location.getLineNumber() + ", column: " + location.getColumnNumber();
			} else {
				content += ", line: -1, column: -1";
			}
			if (failOnError) {
				throw new MojoFailureException(content);
			}
			getLog().error(content);
		}
	}

	private String getId(org.eclipse.aether.artifact.Artifact artifact) {
		return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":"
				+ artifact.getClassifier();
	}

	private String getId(Dependency dependency) {
		return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion() + ":"
				+ Objects.requireNonNullElse(dependency.getClassifier(), "");
	}

	private static final class TrackedDependency {

		private final String id;
		private final InputLocation location;
		private final File file;

		public TrackedDependency(String id, InputLocation location, File file) {
			this.id = id;
			this.location = location;
			this.file = file;
		}

		@Override
		public int hashCode() {
			return Objects.hash(file);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TrackedDependency other = (TrackedDependency) obj;
			return Objects.equals(file, other.file);
		}

	}

}
