/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *                       - Issue #443 - Use regular Maven coordinates -when possible- for dependencies
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Collection;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;

public interface P2ResolverFactory {

    /**
     * Creates a new object for collecting the bundles within the POM dependencies.
     */
    // TODO move to a PomDependencyCollectorFactory interface?
    public PomDependencyCollector newPomDependencyCollector(ReactorProject project);

    /**
     * Create a resolver for the given environments
     * 
     * @param environments
     * 
     * @return a resolver that is capable of resolving the provided environments
     */
    public P2Resolver createResolver(Collection<TargetEnvironment> environments);

    /**
     * tries to resolve a {@link MavenDependencyDescriptor} from the given
     * {@link ArtifactDescriptor}. If the {@link ArtifactDescriptor} does not contain any
     * information about the maven origin <code>null</code> is returned.
     * 
     * @param artifactDescriptor
     * @return the resolved maven origin or <code>null</code> if none could be found in the metadata
     */
    MavenDependencyDescriptor resolveDependencyDescriptor(ArtifactDescriptor artifactDescriptor);

}
