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
package org.eclipse.tycho.core.resolver.shared;

import java.net.URI;

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

}
