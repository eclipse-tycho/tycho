/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - moved resolution context out of p2 resolver
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver.facade;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;

/**
 * The resolution context is the content against which the dependencies of a project can be
 * resolved. For each project, a resolution context is created according to the target platform
 * configuration. Then the p2 resolver narrows it down to create the actual "target platform". The
 * target platform is hence a subset of the resolution context.
 * 
 * @see P2Resolver
 * @see P2ResolutionResult
 * @see org.eclipse.tycho.core.TargetPlatform
 */
public interface ResolutionContext {
    public void addReactorArtifact(IReactorArtifactFacade project);

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact);

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile);

    public void addP2Repository(URI location);

    // TODO document
    public void addTargetDefinition(TargetDefinition definition, List<Map<String, String>> environments)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException;

    public void setCredentials(URI location, String username, String password);

    /**
     * Releases all resources used by the resolver instance
     */
    public void stop();
}
