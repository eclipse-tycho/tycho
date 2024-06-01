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
package org.eclipse.tycho;

import java.io.File;
import java.util.Objects;

public interface ResolvedArtifactKey extends ArtifactKey {

    File getLocation();

    static ResolvedArtifactKey of(ArtifactKey key, File location) {
        return of(key.getType(), key.getId(), key.getVersion(), location);
    }

    static ResolvedArtifactKey bundle(String id, String version, File location) {
        return of(ArtifactType.TYPE_ECLIPSE_PLUGIN, id, version, location);
    }

    static ResolvedArtifactKey of(String type, String id, String version, File location) {
        Objects.requireNonNull(location);
        if (!location.exists()) {
            throw new IllegalArgumentException("location " + location.getAbsolutePath() + " does not exist!");
        }
        return new ResolvedArtifactKey() {

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public File getLocation() {
                return location;
            }

            @Override
            public String toString() {
                return "[" + getType() + "] " + getId() + " " + getVersion() + " @ " + getLocation();
            }
        };
    }
}
