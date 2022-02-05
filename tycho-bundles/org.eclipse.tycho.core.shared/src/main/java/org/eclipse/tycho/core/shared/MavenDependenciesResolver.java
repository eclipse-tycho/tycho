/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.shared;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface MavenDependenciesResolver {
    /**
     * Only the specified artifact will be resolved
     */
    int DEEP_NO_DEPENDENCIES = 0;
    /**
     * Only the specified artifact and its direct child dependencies will be resolved
     */
    int DEEP_DIRECT_CHILDREN = 2;

    /**
     * All transitive dependencies will be resolved
     */
    int DEEP_INFINITE = Integer.MAX_VALUE;

    /**
     * Resolves the given artifact, optionally with the given dependency scope
     *
     * @param groupId
     *            group id of the artifact, required
     * @param artifactId
     *            artifact id of the artifact, required
     * @param version
     *            version of the artifact, required
     * @param packaging
     *            the packaging type, might be <code>null</code> in witch case "jar" is assumed
     * @param classifier
     *            the classifier or <code>null</code> if no classifier is desired
     * @param collection
     *            optional dependency scope, if given it tries to resolve transitive dependencies of
     *            the given artifact as well
     * @param additionalRepositories
     *            additional repositories to use in the resolve process
     * @return
     * @throws DependencyResolutionException
     */
    default Collection<? /* IArtifactFacade */> resolve(String groupId, String artifactId, String version,
            String packaging, String classifier, Collection<String> scopes, int depth,
            Collection<MavenArtifactRepositoryReference> additionalRepositories) throws DependencyResolutionException {
        return resolve(groupId, artifactId, version, packaging, classifier, scopes, depth, additionalRepositories,
                null);
    }

    Collection<? /* IArtifactFacade */> resolve(String groupId, String artifactId, String version, String packaging,
            String classifier, Collection<String> scopes, int depth,
            Collection<MavenArtifactRepositoryReference> additionalRepositories,
            Object/* MavenSession */ session) throws DependencyResolutionException;

    File getRepositoryRoot();

    /**
     * This reads the given artifactFacade as a maven (pom) model and returns the list of declared
     * dependencies.
     *
     * @param artifactFacade
     * @return a list of dependencies for this artifactFacade
     */
    MavenModelFacade loadModel(File modelFile) throws IOException;

}
