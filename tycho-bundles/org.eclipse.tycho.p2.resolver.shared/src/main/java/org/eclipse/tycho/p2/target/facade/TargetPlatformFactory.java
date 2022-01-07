/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #462 - Delay Pom considered items to the final Target Platform calculation 
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.util.List;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;

// TODO 412416 javadoc
public interface TargetPlatformFactory {

    public TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects);

    public TargetPlatform createTargetPlatformWithUpdatedReactorContent(TargetPlatform baseTargetPlatform,
            List<?/* PublishingRepository */> upstreamProjectResults, PomDependencyCollector pomDependencies);

}
