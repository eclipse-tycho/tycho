/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.util.List;

import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;

// TODO 412416 javadoc
public interface TargetPlatformFactory {

    public TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpParameters,
            ExecutionEnvironmentConfiguration eeConfiguration, List<IReactorArtifactFacade> reactorProjects,
            PomDependencyCollector pomDependencies);

}
