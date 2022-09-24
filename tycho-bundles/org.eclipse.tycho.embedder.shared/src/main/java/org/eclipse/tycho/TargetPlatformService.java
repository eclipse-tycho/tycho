/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho;

import java.util.Optional;

/**
 * {@link TargetPlatformService} allows access to the {@link TargetPlatform} used to resolve
 * dependencies.
 *
 */
public interface TargetPlatformService {

    /**
     * 
     * @param project
     *            the project for what the {@link TargetPlatform} should be queried
     * @return the target platform for the given {@link ReactorProject} or an empty optional if the
     *         project do not has one (e.g. is not a Tycho project type)
     * @throws DependencyResolutionException
     *             when the target platform for the project can not be resolved
     */
    Optional<TargetPlatform> getTargetPlatform(ReactorProject project) throws DependencyResolutionException;

    /**
     * Clears the given target platform for this project
     * 
     * @param reactorProject
     *            the project for what the {@link TargetPlatform} should be cleared
     */
    void clearTargetPlatform(ReactorProject reactorProject);
}
