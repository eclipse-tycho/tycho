/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

public interface ReactorRepositoryManager extends ReactorRepositoryManagerFacade {

    IProvisioningAgent getAgent();

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            a reference to a project in the reactor.
     */
    @Override
    PublishingRepository getPublishingRepository(ReactorProjectIdentities project);

}
