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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

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

	@Component
	protected TychoProjectManager projectManager;
	@Component
	private Logger logger;

	@Component
	private Map<String, ArtifactBaselineComparator> comparators;

	private ThreadLocal<IInstallableUnit> contextIu = new ThreadLocal<IInstallableUnit>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || baselines == null || baselines.isEmpty()) {
			return;
		}
		Optional<ArtifactKey> artifactKeyLookup = projectManager.getArtifactKey(project);
		if (artifactKeyLookup.isEmpty()) {
			return;
		}
		ArtifactKey artifactKey = artifactKeyLookup.get();
		ArtifactBaselineComparator comparator = comparators.get(artifactKey.getType());
		if (comparator == null) {
			// nothing to compare...
			return;
		}
		for (Repository repository : baselines) {
			if (repository.getUrl() == null || repository.getUrl().isBlank()) {
				continue;
			}
			try {
				IMetadataRepository metadataRepositor = repositoryManager.getMetadataRepositor(repository);
				IArtifactRepository artifactRepository = repositoryManager.getArtifactRepository(repository);
				org.osgi.framework.Version artifactVersion = org.osgi.framework.Version
						.parseVersion(artifactKey.getVersion());
				Version maxVersion = Version.createOSGi(artifactVersion.getMajor(), artifactVersion.getMinor(),
						artifactVersion.getMicro() + 1);
				IQueryResult<IInstallableUnit> result = metadataRepositor
						.query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(artifactKey.getId(),
								new VersionRange(Version.emptyVersion, true, maxVersion, false))), null);
				if (result.isEmpty()) {
					continue;
				}
				IInstallableUnit iu = comparator.selectIU(result);
				if (iu == null) {
					continue;
				}
				logger.info("Comparing artifact " + artifactKey + " against baseline " + iu.getId() + ":"
						+ iu.getVersion() + " from " + repository.getUrl());
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				repositoryManager.downloadArtifact(iu, artifactRepository, outputStream);
				contextIu.set(iu);
				comparator.compare(DefaultReactorProject.adapt(project),
						() -> new ByteArrayInputStream(outputStream.toByteArray()),
						this);
				return;
			} catch (URISyntaxException e) {
				throw new MojoExecutionException("Repository " + repository + " uses an invalid URI: " + e.getReason(),
						e);
			} catch (ProvisionException e) {
				throw new MojoExecutionException(
						"Loading repository " + repository + "failed! (" + e.getMessage() + ")", e);
			} catch (MojoFailureException e) {
				throw e;
			} catch (MojoExecutionException e) {
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoExecutionException(e);
			}
		}
		String message = "No baseline artifact found!";
		if (mode == BaselineMode.evolve) {
			logger.info(message);
			return;
		}
		reportBaselineProblem(message);
	}

	@Override
	public void reportBaselineProblem(String message) throws MojoFailureException {
		if (mode == BaselineMode.warn) {
			logger.warn(message);
		} else {
			throw new MojoFailureException(message);
		}
	}

	@Override
	public IInstallableUnit getUnit() {

		return contextIu.get();
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

}
