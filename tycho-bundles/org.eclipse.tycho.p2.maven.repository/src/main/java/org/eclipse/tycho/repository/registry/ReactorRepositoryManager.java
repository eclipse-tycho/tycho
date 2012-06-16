/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.registry;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

public interface ReactorRepositoryManager extends ReactorRepositoryManagerFacade {

    IProvisioningAgent getAgent();

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            a project in the reactor.
     */
    PublishingRepository getPublishingRepository(ReactorProjectCoordinates project);

    /**
     * Returns a view onto the project's publishing repository which allows writing new artifacts.
     * 
     * @param project
     *            a project in the reactor.
     */
    PublishingRepository getPublishingRepositoryForArtifactWriting(ReactorProjectCoordinates project,
            WriteSessionContext writeSession);

}
