/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.io.File;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;

/**
 * Object that allows to collect POM dependency artifacts and their p2 metadata.
 * 
 * @see org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory#newPomDependencyCollector()
 */
public interface PomDependencyCollector {

    // TODO 412416 get rid of this method
    /**
     * Sets the root folder of the project the target platform applies to.
     */
    public void setProjectLocation(File projectLocation);

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact);

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile);

}
