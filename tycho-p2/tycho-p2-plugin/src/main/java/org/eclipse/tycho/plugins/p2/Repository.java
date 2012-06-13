/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.net.URI;

import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;

// TODO reconcile with MavenRepositoryLocation
public class Repository {

    private String id;
    private URI url;

    // no-args constructor is used by parameter injection
    public Repository() {
    }

    public URI getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public MavenRepositoryLocation toRepositoryLocation() {
        return new MavenRepositoryLocation(id, url);
    }
}
