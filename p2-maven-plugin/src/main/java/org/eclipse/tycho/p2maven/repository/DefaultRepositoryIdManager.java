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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.p2maven.helper.P2PasswordUtil;

/**
 * Helper class for the Remote*RepositoryManagers taking care of mapping repository URLs to the
 * settings.xml-configured mirrors and setting passwords.
 */
@Component(role = IRepositoryIdManager.class)
public class DefaultRepositoryIdManager implements IRepositoryIdManager {

	@Requirement
	private MavenRepositorySettings settings;
	@Requirement
	private Logger logger;

    private Map<URI, String> knownMavenRepositoryIds = new ConcurrentHashMap<>();

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
        setPasswordForLoading(effectiveLocation);
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

    /**
     * Sets passwords configured in the Maven settings in p2.
     * 
     * <p>
     * Warning: This method heavily relies on side-effects. Instead of remembering the credentials
     * just for the given location, p2 associates the password with the host. This allows to load
     * children of a composite repository with the same credentials as the parent, without having to
     * specify all children in the Maven settings. This feature can easily break if repositories are
     * loaded in parallel. If this shall be supported, a lock is needed here (TODO).
     */
    private void setPasswordForLoading(MavenRepositoryLocation location) {
        MavenRepositorySettings.Credentials credentials = settings.getCredentials(location);
        if (credentials != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting credentials for p2 repository '" + location.getId() + "'");
            }
            P2PasswordUtil.setCredentials(location.getURL(), credentials.getUserName(), credentials.getPassword(),
                    logger);
        }
    }

    private static boolean certainlyNoRemoteURL(URI location) {
        // e.g. in-memory composite artifact repositories; see see org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository.createMemoryComposite(IProvisioningAgent)
        return location.isOpaque() || !location.isAbsolute();
    }

    @Override
	public Stream<MavenRepositoryLocation> getKnownMavenRepositoryLocations() {
        return knownMavenRepositoryIds.entrySet().stream()
                .map(e -> new MavenRepositoryLocation(e.getValue(), e.getKey()));
    }
}
