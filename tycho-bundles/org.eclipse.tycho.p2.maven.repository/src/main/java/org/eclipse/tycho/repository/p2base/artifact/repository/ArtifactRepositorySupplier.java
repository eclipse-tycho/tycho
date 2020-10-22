/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.tycho.repository.p2base.artifact.repository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public interface ArtifactRepositorySupplier extends Supplier<Collection<IArtifactRepository>> {

    default Collection<URI> getRepositoryURLs() {
        return Collections.emptySet();
    }

    static ArtifactRepositorySupplier composite(ArtifactRepositorySupplier... suppliers) {
        return new ArtifactRepositorySupplier() {

            @Override
            public Collection<IArtifactRepository> get() {
                List<IArtifactRepository> list = new ArrayList<>();
                for (ArtifactRepositorySupplier supplier : suppliers) {
                    list.addAll(supplier.get());
                }
                return list;
            }

            @Override
            public Collection<URI> getRepositoryURLs() {
                List<URI> list = new ArrayList<>();
                for (ArtifactRepositorySupplier supplier : suppliers) {
                    list.addAll(supplier.getRepositoryURLs());
                }
                return list;
            }
        };
    }
}
