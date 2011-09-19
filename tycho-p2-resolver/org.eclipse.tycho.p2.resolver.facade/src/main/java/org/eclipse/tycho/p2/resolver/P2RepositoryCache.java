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
package org.eclipse.tycho.p2.resolver;

import java.net.URI;

/**
 * This is only used to cache IArtifactRepository and IMetadataRepository instances corresponding to
 * remote Maven/Tycho repositories introduced in Tycho 0.4.0. This repository format is disabled by
 * default since Tycho 0.8.0.
 * 
 * @see https://issues.sonatype.org/browse/TYCHO-335
 * 
 * @TODO I am not sure we actually still need this
 * @author igor
 */
public interface P2RepositoryCache {

    public Object getMetadataRepository(URI location);

    public Object getArtifactRepository(URI location);

    public void putRepository(URI location, Object metadataRepository, Object artifactRepository);

}
