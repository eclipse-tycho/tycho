/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;

/**
 * Object that allows to collect POM dependency artifacts and their p2 metadata.
 * 
 * @see org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory#newPomDependencyCollector()
 */
public interface PomDependencyCollector {

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact);

    public void publishAndWrapArtifactIfNeccesary(IArtifactFacade artifact);

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile);

}
