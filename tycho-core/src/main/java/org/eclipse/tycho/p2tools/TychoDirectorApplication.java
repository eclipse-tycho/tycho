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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

/**
 * Enhances the P2 {@link DirectorApplication} by injecting the agent provider from plexus context,
 * additionally to that ensures that the extension classloader is used to load this class as it is
 * part of tycho-core module.
 */
public class TychoDirectorApplication extends DirectorApplication {

    public TychoDirectorApplication(IProvisioningAgentProvider agentProvider, IProvisioningAgent agent) {
        //TODO should be able to control agent creation see https://github.com/eclipse-equinox/p2/pull/398
        //Until now we need to fetch a service to trigger loading of the internal osgi framework...
        agent.getService(IArtifactRepositoryManager.class);
    }
}
