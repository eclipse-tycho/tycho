/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfiguration;

public class TychoProjectUtils {
    private static final String TYCHO_NOT_CONFIGURED = "Tycho build extension not configured for ";

    /**
     * Returns the {@link DependencyArtifacts} instance associated with the given project.
     * 
     * @param project
     *            a Tycho project
     * @return the resolved dependencies of the given project; never <code>null</code>
     * @throws IllegalStateException
     *             if the given project does not have the resolved project dependencies stored
     */
    public static DependencyArtifacts getDependencyArtifacts(MavenProject project) throws IllegalStateException {
        DependencyArtifacts resolvedDependencies = (DependencyArtifacts) project
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        if (resolvedDependencies == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return resolvedDependencies;
    }

    /**
     * Returns the {@link TargetPlatformConfiguration} instance associated with the given project.
     * 
     * @param project
     *            a Tycho project
     * @return the target platform configuration for the given project; never <code>null</code>
     * @throws IllegalStateException
     *             if the given project does not have an associated target platform configuration
     */
    public static TargetPlatformConfiguration getTargetPlatformConfiguration(MavenProject project)
            throws IllegalStateException {
        TargetPlatformConfiguration targetPlatformConfiguration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        if (targetPlatformConfiguration == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatformConfiguration;
    }

    public static TargetPlatform getTargetPlatform(MavenProject project) {
        TargetPlatform targetPlatform = (TargetPlatform) project.getContextValue(TychoConstants.CTX_TARGET_PLATFORM);
        if (targetPlatform == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatform;
    }

    public static ExecutionEnvironmentConfiguration getExecutionEnvironmentConfiguration(MavenProject project) {
        ExecutionEnvironmentConfiguration storedConfig = (ExecutionEnvironmentConfiguration) project
                .getContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION);
        if (storedConfig == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return storedConfig;
    }
}
