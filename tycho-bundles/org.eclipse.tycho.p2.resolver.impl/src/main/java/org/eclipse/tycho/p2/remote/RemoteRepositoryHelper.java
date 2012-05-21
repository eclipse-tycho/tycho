/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;

class RemoteRepositoryHelper implements IRepositoryIdManager {

    private final MavenRepositorySettings settings;
    private final MavenLogger logger;

    private Map<URI, String> knownMavenRepositoryIds = new HashMap<URI, String>();

    public RemoteRepositoryHelper(MavenRepositorySettings settings, MavenLogger logger) {
        this.settings = settings;
        this.logger = logger;
    }

    public void addMapping(String mavenRepositoryId, URI location) {
        if (mavenRepositoryId == null)
            return;

        URI key = normalize(location);
//        boolean overwrite =  knownLocations.containsKey(key);
        String previousValue = knownMavenRepositoryIds.put(key, mavenRepositoryId);

        if (previousValue != null && !(mavenRepositoryId.equals(previousValue))) {
            logger.warn("p2 repository with URL " + key + " is associated with multiple IDs; was '" + previousValue
                    + "', now is '" + mavenRepositoryId + "'");
        }
    }

    private static URI normalize(URI location) {
        // make sure path ends in '/'
        try {
            if (location.getPath() == null) {
                return new URI(location.getScheme(), location.getAuthority(), "/", location.getQuery(),
                        location.getFragment());
            } else if (!location.getPath().endsWith("/")) {
                return new URI(location.getScheme(), location.getAuthority(), location.getPath() + "/",
                        location.getQuery(), location.getFragment());
            } else {
                return location;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URI getEffectiveLocation(URI location) {
        MavenRepositoryLocation effectiveLocation = effectiveLocationFor(location);
        return effectiveLocation.getURL();
    }

    public URI getEffectiveLocationAndPrepareLoad(URI location) throws ProvisionException {
        MavenRepositoryLocation effectiveLocation = effectiveLocationFor(location);

        setPasswordForLoading(effectiveLocation);

        return effectiveLocation.getURL();
    }

    private MavenRepositoryLocation effectiveLocationFor(URI location) {
        URI normalizedLocation = normalize(location);
        String id = knownMavenRepositoryIds.get(normalizedLocation);
        MavenRepositoryLocation locationWithID = new MavenRepositoryLocation(id, normalizedLocation);

        MavenRepositoryLocation mirror = settings.getMirror(locationWithID);
        if (mirror != null)
            return mirror;
        else {
            return locationWithID;
        }
    }

    private void setPasswordForLoading(MavenRepositoryLocation location) throws ProvisionException {
        MavenRepositorySettings.Credentials credentials = settings.getCredentials(location);
        if (credentials != null) {
            P2PasswordUtil.setCredentials(location.getURL(), credentials.getUserName(), credentials.getPassword(),
                    logger);
        }
    }

}
