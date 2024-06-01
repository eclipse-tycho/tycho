/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - Bug 453708 Support for site/repository-reference/@location in eclipse-repository
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

import java.net.URI;

public record RepositoryReference(String name, String location, boolean enable) {

    public URI locationURINormalized() {
        // P2 does the same before loading the repo and thus IRepository.getLocation() returns the normalized URL.
        // In order to avoid stripping of slashes from URI instances do it now before URIs are created.
        return URI.create(location.endsWith("/") ? location.substring(0, location.length() - 1) : location);
    }
}
