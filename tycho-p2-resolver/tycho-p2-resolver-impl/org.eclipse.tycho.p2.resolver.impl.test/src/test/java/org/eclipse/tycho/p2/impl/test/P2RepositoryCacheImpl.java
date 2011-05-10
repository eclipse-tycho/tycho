/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import java.net.URI;

import org.eclipse.tycho.p2.resolver.facade.P2RepositoryCache;

public class P2RepositoryCacheImpl implements P2RepositoryCache {

    public Object getMetadataRepository(URI location) {
        return null;
    }

    public Object getArtifactRepository(URI location) {
        return null;
    }

    public void putRepository(URI location, Object metadataRepository, Object artifactRepository) {
    }

}
