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
 *    Christoph Läubrich - Issue #697 - Failed to resolve dependencies with Tycho 2.7.0 for custom repositories
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

@Component(role = ReactorRepositoryManager.class)
public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    @Requirement
    IProvisioningAgent agent;

    @Override
    public PublishingRepository getPublishingRepository(ReactorProjectIdentities project) {
        return new PublishingRepositoryImpl(agent, project);
    }

}
