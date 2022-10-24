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
package org.eclipse.tycho.p2maven.transport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.internal.p2.repository.Transport")
public class TychoRepositoryTransportAgentFactory implements IAgentServiceFactory, Initializable {

	@Requirement(hint = "connect")
    private EquinoxServiceFactory serviceFactory;

	@Requirement
	private LegacySupport legacySupport;

    @Requirement
    private MavenRepositorySettings mavenRepositorySettings;
	@Requirement
	private Logger logger;

	private File repoDir;

	private boolean offline;
	private boolean update;

	private List<MavenRepositoryLocation> repositoryLocations;

    @Override
    public Object createService(IProvisioningAgent agent) {

		File cacheLocation = new File(repoDir, ".cache/tycho");
		cacheLocation.mkdirs();
		logger.info("### Using TychoRepositoryTransport for remote P2 access ###");
		logger.info("    Cache location:         " + cacheLocation);
		logger.info("    Transport mode:         " + (offline ? "offline" : "online"));
		logger.info("    Update mode:            " + (update ? "forced" : "cache first"));
		logger.info("    Minimum cache duration: " + SharedHttpCacheStorage.MIN_CACHE_PERIOD + " minutes");
		logger.info(
				"      (you can configure this with -Dtycho.p2.transport.min-cache-minutes=<desired minimum cache duration>)");

		SharedHttpCacheStorage cache = SharedHttpCacheStorage.getStorage(cacheLocation, offline, update);

		return new TychoRepositoryTransport(logger, serviceFactory.getService(IProxyService.class), cache, uri -> {
            IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
			Stream<MavenRepositoryLocation> locations = repositoryLocations.stream();
            locations = Stream.concat(locations, repositoryIdManager.getKnownMavenRepositoryLocations());
            String requestUri = uri.normalize().toASCIIString();
            return locations.sorted((loc1, loc2) -> {
                //we wan't the longest prefix match, so first sort all uris by their length ...
                String s1 = loc1.getURL().normalize().toASCIIString();
                String s2 = loc2.getURL().normalize().toASCIIString();
                return Long.compare(s2.length(), s1.length());
            }).filter(loc -> {
                String prefix = loc.getURL().normalize().toASCIIString();
                return requestUri.startsWith(prefix);
            }).map(mavenRepositorySettings::getCredentials).filter(Objects::nonNull).findFirst().orElse(null);
        });
    }

	@Override
	public void initialize() throws InitializationException {
		MavenSession session = legacySupport.getSession();
		if (session == null) {
			repoDir = RepositorySystem.defaultUserLocalRepository;
			offline = false;
			update = false;
			repositoryLocations = List.of();
		} else {
			offline = session.isOffline();
			repoDir = new File(session.getLocalRepository().getBasedir());
			update = session.getRequest().isUpdateSnapshots();
			List<MavenProject> projects = Objects.requireNonNullElse(session.getProjects(), Collections.emptyList());
			repositoryLocations = projects.stream()
					.map(MavenProject::getRemoteArtifactRepositories).flatMap(Collection::stream)
					.filter(r -> r.getLayout() instanceof P2ArtifactRepositoryLayout).map(r -> {
						try {
							return new MavenRepositoryLocation(r.getId(), new URL(r.getUrl()).toURI());
						} catch (MalformedURLException | URISyntaxException e) {
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
		}

	}

}
