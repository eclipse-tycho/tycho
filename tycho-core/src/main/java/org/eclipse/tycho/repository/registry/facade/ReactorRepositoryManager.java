/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.repository.PublishingRepository;

/**
 * Manages the p2 repositories for the projects' build results ("publishing repository") and the p2
 * repositories with the projects' context artifacts ("target platform").
 */
public interface ReactorRepositoryManager {

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            a reference to a project in the reactor.
     * @return the {@link PublishingRepository} for the {@link ReactorProjectIdentities}
     */
    PublishingRepository getPublishingRepository(ReactorProjectIdentities project);

    PublishingRepository getPublishingRepository(ReactorProject project);

}
