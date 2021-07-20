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
import java.util.Collection;
import java.util.Collections;

public interface MavenDependenciesResolver {

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
     *            the packaging type, might me <code>null</code> in witch case "jar" is assumed
     * @param classifier
     *            the classifier or <code>null</code> if no classifier is desired
     * @param dependencyScope
     *            optional dependency scope, if given it tries to resolve transitive dependencies of
     *            the given artifact as well
     * @return
     */
    default Collection<? /* IArtifactFacade */> resolve(String groupId, String artifactId, String version,
            String packaging, String classifier, String dependencyScope) {
        return resolve(groupId, artifactId, version, packaging, classifier, dependencyScope, Collections.emptyList());
    }

    Collection<? /* IArtifactFacade */> resolve(String groupId, String artifactId, String version, String packaging,
            String classifier, String dependencyScope,
            Collection<MavenArtifactRepositoryReference> additionalRepositories);

    File getRepositoryRoot();

}
