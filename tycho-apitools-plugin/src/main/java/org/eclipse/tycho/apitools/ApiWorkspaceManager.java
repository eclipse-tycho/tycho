/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.apitools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The {@link ApiWorkspaceManager} manages dedicated workspaces on a per thread
 * basis
 */
@Component(role = ApiWorkspaceManager.class)
public class ApiWorkspaceManager implements Disposable {

	static final String BUNDLE_APP = "org.eclipse.equinox.app";

	static final String BUNDLE_SCR = "org.apache.felix.scr";

	static final String BUNDLE_CORE = "org.eclipse.core.runtime";

	private static final Set<String> START_BUNDLES = Set.of(BUNDLE_CORE, BUNDLE_SCR, BUNDLE_APP);

	private final Map<Thread, Map<URI, ApiWorkspace>> cache = new ConcurrentHashMap<>();

	private Set<Path> init = ConcurrentHashMap.newKeySet();

	@Requirement
	private ApiApplicationResolver applicationResolver;

	@Requirement
	private Logger logger;

	/**
	 * @param apiToolsRepo the API tools repository to use to bootstrap this
	 *                     workspace
	 * @return a workspace directory that can be used by the current thread.
	 */
	public ApiWorkspace getWorkspace(MavenRepositoryLocation apiToolsRepo) {
		return cache.computeIfAbsent(Thread.currentThread(), t -> new ConcurrentHashMap<>())
				.computeIfAbsent(apiToolsRepo.getURL().normalize(), x -> {
					try {
						return new ApiWorkspace(Files.createTempDirectory("apiWorksapce"), apiToolsRepo);
					} catch (IOException e) {
						throw new IllegalStateException("can't create a temporary directory!", e);
					}
				});
	}

	@Override
	public void dispose() {
		cache.values().forEach(map -> {
			map.values().forEach(ws -> FileUtils.deleteQuietly(ws.workDir.toFile()));
		});
	}

	final class ApiWorkspace {

		private Path workDir;
		private MavenRepositoryLocation apiToolsRepo;

		private ApiWorkspace(Path workDir, MavenRepositoryLocation apiToolsRepo) {
			this.workDir = workDir;
			this.apiToolsRepo = apiToolsRepo;
		}

		public Path getWorkDir() {
			return workDir;
		}

		public void install(BundleContext bundleContext) throws IOException, BundleException {
			if (init.add(workDir)) {
				for (Path bundleFile : applicationResolver.getApiApplicationBundles(apiToolsRepo)) {
					Bundle bundle;
					try (InputStream stream = Files.newInputStream(bundleFile)) {
						bundle = bundleContext.installBundle(bundleFile.toUri().toString(), stream);
					} catch (BundleException e) {
						logger.warn("Can't install " + bundleFile + ": " + e);
						continue;
					}
					if (START_BUNDLES.contains(bundle.getSymbolicName())) {
						bundle.start();
					}
				}
			}
		}

	}
}
