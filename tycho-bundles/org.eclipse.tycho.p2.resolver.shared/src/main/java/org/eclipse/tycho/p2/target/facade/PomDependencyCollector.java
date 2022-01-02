/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *                       - Issue #462 - Delay Pom considered items to the final Target Platform calculation 
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.util.Map;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

/**
 * Object that allows to collect POM dependency artifacts and their p2 metadata.
 * 
 * @see org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory#newPomDependencyCollector()
 */
public interface PomDependencyCollector {

    public void addMavenArtifact(IArtifactFacade artifact, boolean allowGenerateOSGiBundle);

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile);

    Map<?/* IInstallableUnit */, IArtifactFacade> getMavenInstallableUnits();

    ArtifactKey getArtifactKey(IArtifactFacade facade);

}
