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
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
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
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

/**
 * Verifies the artifact against a given baseline repository for version
 * changes.
 *
 */
@Mojo(defaultPhase = LifecyclePhase.VERIFY, name = "verify", threadSafe = true)
public class BaselineMojo extends aQute.bnd.maven.baseline.plugin.BaselineMojo {
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

	@Parameter(property = "tycho.baseline.skip", defaultValue = "false")
	private boolean skip;

	@Parameter(property = "tycho.baseline.fail.on.missing", defaultValue = "true")
	private boolean failOnMissing;

	@Component
	protected TychoProjectManager projectManager;
	@Component
	private Logger logger;

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
		for (Repository repository : baselines) {
			try {
				IMetadataRepository metadataRepositor = repositoryManager.getMetadataRepositor(repository);
				IArtifactRepository artifactRepository = repositoryManager.getArtifactRepository(repository);
				IQueryResult<IInstallableUnit> result = metadataRepositor.query(
						QueryUtil.createLatestQuery(QueryUtil.createIUQuery(artifactKey.getId(),
								new VersionRange(Version.emptyVersion, true, Version.create(artifactKey.getVersion()),
										false))),
						null);

				for (IInstallableUnit iu : result) {
					File tempFile = File.createTempFile("baseline", ".jar");
					tempFile.deleteOnExit();
					try (FileOutputStream stream = new FileOutputStream(tempFile)) {
						Object status = repositoryManager.downloadArtifact(iu, artifactRepository, stream);
						System.out.println(status);
					}
					// hack... see https://github.com/bndtools/bnd/pull/5441
					Method method = aQute.bnd.maven.baseline.plugin.BaselineMojo.class
							.getDeclaredMethod("baselineAction", File.class, File.class);
					method.setAccessible(true);
					method.invoke(this, project.getArtifact().getFile(), tempFile);
					return;
				}
			} catch (URISyntaxException e) {
				throw new MojoExecutionException("Repository " + repository + " uses an invalid URI: " + e.getReason(),
						e);
			} catch (ProvisionException e) {
				throw new MojoExecutionException(
						"Loading repository " + repository + "failed! (" + e.getMessage() + ")", e);
			} catch (Exception e) {
				throw new MojoExecutionException(e);
			}
		}
		if (failOnMissing) {
			throw new MojoFailureException("No baseline artifact found!");
		}
	}
}
