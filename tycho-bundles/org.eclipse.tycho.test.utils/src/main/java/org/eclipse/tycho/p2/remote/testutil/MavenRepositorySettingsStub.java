/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
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
package org.eclipse.tycho.p2.remote.testutil;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;

public class MavenRepositorySettingsStub implements MavenRepositorySettings {
    private Map<String, URI> idToMirrorMap = new HashMap<>();

    public void addMirror(String repositoryId, URI mirroredUrl) {
        idToMirrorMap.put(repositoryId, mirroredUrl);
    }

    @Override
    public MavenRepositoryLocation getMirror(MavenRepositoryLocation repository) {
        if (idToMirrorMap.containsKey(repository.getId())) {
            return new MavenRepositoryLocation("mirror-id", idToMirrorMap.get(repository.getId()));
        }
        return null;
    }

    @Override
    public Credentials getCredentials(MavenRepositoryLocation location) {
        return null;
    }

}
