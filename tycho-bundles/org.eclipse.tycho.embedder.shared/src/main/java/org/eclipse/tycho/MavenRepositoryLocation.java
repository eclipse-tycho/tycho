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
package org.eclipse.tycho;

import java.net.URI;

// TODO javadoc
public class MavenRepositoryLocation {

    private final String id;
    private final URI location;

    public MavenRepositoryLocation(String id, URI location) {
        this.id = id;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public URI getURL() {
        return location;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MavenRepositoryLocation [");
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (location != null) {
            builder.append("location=");
            builder.append(location);
        }
        builder.append("]");
        return builder.toString();
    }

}
