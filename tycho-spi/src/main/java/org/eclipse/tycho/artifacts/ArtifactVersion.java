/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.artifacts;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.osgi.framework.Version;

public interface ArtifactVersion {

    Path getArtifact();

    Version getVersion();

    String getProvider();

    /**
     * Returns fragment artifacts compatible with this bundle version
     *
     * @return a stream of artifact versions for compatible fragments, empty if
     *         this is not a host bundle or no fragments are found
     */
    default Stream<ArtifactVersion> fragments() {
        return Stream.empty();
    }
}
