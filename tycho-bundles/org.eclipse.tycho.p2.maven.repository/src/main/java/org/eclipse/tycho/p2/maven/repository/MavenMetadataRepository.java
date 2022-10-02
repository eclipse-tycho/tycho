/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.net.URI;

import org.eclipse.tycho.p2.repository.AbstractMavenMetadataRepository;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class MavenMetadataRepository extends AbstractMavenMetadataRepository {

    public MavenMetadataRepository(URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator) {
        super(Activator.getProvisioningAgent(), location, projectIndex, contentLocator);
    }
}
