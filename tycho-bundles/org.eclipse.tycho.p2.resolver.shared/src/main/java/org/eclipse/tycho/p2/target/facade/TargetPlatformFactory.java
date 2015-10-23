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

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;

public interface TargetPlatformFactory {

    /**
     * Creates a new object for collecting the bundles within the POM dependencies.
     */
    public PomDependencyCollector newPomDependencyCollector();

    /**
     * Computes the target platform from the given configuration and content.
     * 
     * @param tpConfiguration
     * @param eeConfiguration
     *            The target execution environment profile.
     * @param reactorProjects
     *            may be <code>null</code>
     * @param pomDependencies
     *            may be <code>null</code>
     * @param logConfig
     *            may be <code>null</code>
     */
    public TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects,
            PomDependencyCollector pomDependencies, LogConfiguration logConfig);

    public interface LogConfiguration {

        boolean diskLoggingEnabled();

        String getFilePrefix();

    }

}
