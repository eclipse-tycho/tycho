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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.exceptions.VersionBumpRequiredException;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.osgi.framework.Version;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Verifies the artifact against a given baseline repository for version
 * changes.
 *
 */
@Mojo(defaultPhase = LifecyclePhase.VERIFY, name = "verify", threadSafe = true)
public class BaselineMojo extends AbstractMojo implements BaselineContext {
	/**
	 * A list of p2 repositories to be used as baseline. Those are typically the
	 * most recent released versions of your project.
	 */
	@Parameter(property = "baselines", name = "baselines")
	private List<Repository> baselines;

	@Component
	private P2RepositoryManager repositoryManager;

	@Parameter(property = "session", readonly = true)
	private MavenSession session;

	@Parameter(property = "project", readonly = true)
	protected MavenProject project;

	/**
	 * If <code>true</code> skips any baseline processing.
	 */
	@Parameter(property = "tycho.baseline.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Controls if the mojo should fail or only warn about baseline problems.
	 */
	@Parameter(property = "tycho.baseline.mode", defaultValue = "evolve")
	private BaselineMode mode = BaselineMode.evolve;

	/**
	 * Defines what packages should be compared to the baseline, by default all
	 * exported packages are compared. Packages can contain wildcards, for a full
	 * list of supported syntax see
	 * https://bnd.bndtools.org/chapters/820-instructions.html#selector
	 */
	@Parameter(property = "tycho.baseline.packages")
	private List<String> packages;

	/**
	 * Defines manifest header names or resource paths that should be ignore when
	 * comparing to the baseline.
	 */
	@Parameter(property = "tycho.baseline.diffignores")
	private List<String> ignores;

	/**
	 * If <code>true</code> enables processing of eclipse specific extensions e.g
	 * x-internal directive.
	 */
	@Parameter(property = "tycho.baseline.extensions", defaultValue = "false")
	private boolean extensions;

	/**
	 * Configure the step size for the micro version that is suggested as an
	 * increment.
	 */
	@Parameter(property = "tycho.baseline.increment", defaultValue = "1")
	private int increment = 1;

	@Component
	protected TychoProjectManager projectManager;
	@Component
	private Logger logger;

	@Component
	private Map<String, ArtifactBaselineComparator> comparators;

	@Component
	BuildContext buildContext;

	@Component
	private BundleReader bundleReader;

	private ThreadLocal<IArtifactRepository> contextArtifactRepository = new ThreadLocal<>();
	private ThreadLocal<IQueryable<IInstallableUnit>> contextMetadataRepository = new ThreadLocal<>();
	private ThreadLocal<ArtifactKey> contexArtifactKey = new ThreadLocal<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO we actually want a method removeAllMessages() so the comparators can add
		// messages on individual files
		buildContext.removeMessages(project.getBasedir());
		if (skip || baselines == null || baselines.isEmpty()) {
			logger.info("Skipped.");
			return;
		}
		Optional<ArtifactKey> artifactKeyLookup = lookupArtifactKey();
		if (artifactKeyLookup.isEmpty()) {
			logger.info("Not an artifact based project.");
			return;
		}
		ArtifactKey artifactKey = artifactKeyLookup.get();
		ArtifactBaselineComparator comparator = comparators.get(artifactKey.getType());
		if (comparator == null) {
			logger.info("Not a baseline comparable project.");
			// nothing to compare...
			return;
		}
		try {
			loadRepositories();
			contexArtifactKey.set(artifactKey);
			if (comparator.compare(project, this)) {
				logger.info("No baseline problems found.");
				return;
			}
		} catch (MojoExecutionException | MojoFailureException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("Unknown error", e);
		} finally {
			contextArtifactRepository.set(null);
			contextMetadataRepository.set(null);
			contexArtifactKey.set(null);
		}
		String message = "No baseline artifact found!";
		if (mode == BaselineMode.evolve) {
			logger.info(message);
			return;
		}
		reportBaselineProblem(message);
	}

	private Optional<ArtifactKey> lookupArtifactKey() {
		Optional<ArtifactKey> key = projectManager.getArtifactKey(project);
		if (key.isEmpty()) {
			// not a default but we should not give up too early!
			if ("jar".equalsIgnoreCase(project.getPackaging()) || "bundle".equalsIgnoreCase(project.getPackaging())) {
				Artifact artifact = project.getArtifact();
				if (artifact != null) {
					File file = artifact.getFile();
					if (file != null && file.isFile()) {
						try {
							OsgiManifest manifest = bundleReader.loadManifest(file);
							return Optional.of(manifest.toArtifactKey());
						} catch (OsgiManifestParserException e) {
							// can't do anything then...
						}
					}
				}
			}
		}
		return key;
	}

	private void loadRepositories() throws MojoExecutionException {
		try {
			contextMetadataRepository.set(repositoryManager.getCompositeMetadataRepository(baselines));
			contextArtifactRepository.set(repositoryManager.getCompositeArtifactRepository(baselines));
		} catch (ProvisionException | URISyntaxException e) {
			throw new MojoExecutionException(
					"Loading baseline repositories "
							+ baselines.stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")) + " failed!",
					e);
		}
	}

	@Override
	public void reportBaselineProblem(String message) throws MojoFailureException {
		reportBaselineProblem(message, null);
	}

	@Override
	public void reportBaselineProblem(String message, Version suggestedVersion) throws MojoFailureException {
		if (mode == BaselineMode.warn) {
			buildContext.addMessage(project.getBasedir(), 0, 0, message, BuildContext.SEVERITY_WARNING, null);
			logger.warn(message);
		} else {
			buildContext.addMessage(project.getBasedir(), 0, 0, message, BuildContext.SEVERITY_ERROR, null);
			if (suggestedVersion != null) {
				throw new VersionBumpRequiredException(message, project, suggestedVersion);
			}
			throw new MojoFailureException(message);
		}
	}

	@Override
	public List<String> getIgnores() {
		if (ignores == null) {
			return List.of();
		}
		return ignores;
	}

	@Override
	public List<String> getPackages() {
		if (packages == null) {
			return List.of("*");
		}
		return packages;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public boolean isExtensionsEnabled() {
		return extensions;
	}

	@Override
	public int getMicroIncrement() {
		return increment;
	}

	@Override
	public IArtifactRepository getArtifactRepository() {
		return contextArtifactRepository.get();
	}

	@Override
	public IQueryable<IInstallableUnit> getMetadataRepository() {
		return contextMetadataRepository.get();
	}

	@Override
	public ArtifactKey getArtifactKey() {
		return contexArtifactKey.get();
	}

}
