/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;

public interface TargetPlatformBuilder {
    /**
     * Sets the root folder of the project the target platform applies to.
     */
    public void setProjectLocation(File projectLocation);

    public void addReactorArtifact(IReactorArtifactFacade project);

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact);

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile);

    public void addP2Repository(MavenRepositoryLocation location);

    // TODO document
    public void addTargetDefinition(TargetDefinition definition, List<Map<String, String>> environments)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException;

    public void addFilters(List<TargetPlatformFilter> filters);

    public TargetPlatform buildTargetPlatform();

    public void setIncludePackedArtifacts(boolean include);

}
