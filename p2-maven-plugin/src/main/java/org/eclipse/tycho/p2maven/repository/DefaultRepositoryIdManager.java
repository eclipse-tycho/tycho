/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;

/**
 * Helper class for the Remote*RepositoryManagers taking care of mapping repository URLs to the
 * settings.xml-configured mirrors and setting passwords.
 */
@Named
@Singleton
public class DefaultRepositoryIdManager implements IRepositoryIdManager {

	@Inject
	private MavenRepositorySettings settings;
	@Inject
	private Logger logger;
	// For some reason maven creates different instances of the component even if
	// there should only be one...
	private static final Map<URI, String> knownMavenRepositoryIds = new ConcurrentHashMap<>();

    @Override
    public void addMapping(String mavenRepositoryId, URI location) {
        if (mavenRepositoryId == null)
            return;

        URI key = normalize(location);
        String previousValue = knownMavenRepositoryIds.put(key, mavenRepositoryId);

        if (previousValue != null && !(mavenRepositoryId.equals(previousValue))) {
            logger.warn("p2 repository with URL " + key + " is associated with multiple IDs; was '" + previousValue
                    + "', now is '" + mavenRepositoryId + "'");
        }
    }

    private static URI normalize(URI location) {
        // remove trailing slashes
        try {
            String path = location.getPath();
            if (path != null && path.endsWith("/")) {
                return new URI(location.getScheme(), location.getAuthority(), path.substring(0, path.length() - 1),
                        location.getQuery(), location.getFragment());
            } else {
                return location;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
	public URI getEffectiveLocation(URI location) {
        if (certainlyNoRemoteURL(location)) {
            return location;
        }

        MavenRepositoryLocation effectiveLocation = effectiveLocationFor(location, false);
        return effectiveLocation.getURL();
    }

    @Override
	public URI getEffectiveLocationAndPrepareLoad(URI location) {
        if (certainlyNoRemoteURL(location)) {
            return location;
        }

        MavenRepositoryLocation effectiveLocation = effectiveLocationFor(location, true);
        return effectiveLocation.getURL();
    }

    private MavenRepositoryLocation effectiveLocationFor(URI location, boolean forLoading) {
        URI normalizedLocation = normalize(location);

        String id = knownMavenRepositoryIds.get(normalizedLocation);
        if (id == null) {
            // fall back to URL as ID
            id = normalizedLocation.toString();
        }
        MavenRepositoryLocation locationWithID = new MavenRepositoryLocation(id, normalizedLocation);

        MavenRepositoryLocation mirror = settings.getMirror(locationWithID);
        if (mirror != null) {
            if (forLoading) {
                logger.info("Loading repository '" + id + "' from mirror '" + mirror.getId() + "' at '"
                        + mirror.getURL() + "'");
            }
            return mirror;
        } else {
            return locationWithID;
        }
    }

    private static boolean certainlyNoRemoteURL(URI location) {
        // e.g. in-memory composite artifact repositories; see see org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository.createMemoryComposite(IProvisioningAgent)
        return location.isOpaque() || !location.isAbsolute();
    }

    @Override
	public Stream<MavenRepositoryLocation> getKnownMavenRepositoryLocations() {
		// Returns both repository and mirror locations
		return Stream.concat(knownMavenRepositoryIds.entrySet().stream()
				.map(e -> new MavenRepositoryLocation(e.getValue(), e.getKey())), settings.getMirrors());
	}

	@Override
	public MavenRepositorySettings getSettings() {
		return settings;
	}

}
