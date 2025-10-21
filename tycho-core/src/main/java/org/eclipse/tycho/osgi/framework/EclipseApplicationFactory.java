/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import java.nio.file.Path;
import java.util.Collection;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.resolver.P2Resolver;

/**
 * Component that resolves all the bundles that make up an Eclipse Application to run from a given
 * URI
 */
public interface EclipseApplicationFactory {

    EclipseApplication createEclipseApplication(MavenRepositoryLocation repositoryLocation, String name);

    EclipseApplication createEclipseApplication(TargetPlatform targetPlatform, String name);

    TargetPlatform createTargetPlatform(Collection<MavenRepositoryLocation> locations);

    Collection<Path> getApiBaselineBundles(Collection<MavenRepositoryLocation> baselineRepoLocations,
            ArtifactKey artifactKey, Collection<TargetEnvironment> environment)
            throws IllegalArtifactReferenceException;

    P2Resolver createResolver();

    P2Resolver createResolver(Collection<TargetEnvironment> environments);

}
