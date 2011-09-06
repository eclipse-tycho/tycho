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
package org.eclipse.tycho.p2.maven.repository;

import java.net.URI;

import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class MavenMetadataRepository extends AbstractMavenMetadataRepository {

    public MavenMetadataRepository(URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator) {
        super(location, projectIndex, contentLocator);
    }
}
