/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testutil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;

public class NoopRepositoryIdManager implements IRepositoryIdManager {

    private IProvisioningAgent agent;

    public NoopRepositoryIdManager(IProvisioningAgent agent) {
        this.agent = agent;
    }

    private Map<URI, String> knownMavenRepositoryIds = new ConcurrentHashMap<>();

    @Override
    public void addMapping(String mavenRepositoryId, URI location) {
        if (mavenRepositoryId == null)
            return;

        URI key = normalize(location);
        knownMavenRepositoryIds.put(key, mavenRepositoryId);

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

        MavenRepositorySettings service = agent.getService(MavenRepositorySettings.class);
        MavenRepositoryLocation mirror = service.getMirror(locationWithID);
        if (mirror != null) {
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
        return knownMavenRepositoryIds.entrySet().stream()
                .map(e -> new MavenRepositoryLocation(e.getValue(), e.getKey()));
    }

}
