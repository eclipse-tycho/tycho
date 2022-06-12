/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;

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
    public static DependencyArtifacts getDependencyArtifacts(ReactorProject project) throws IllegalStateException {
        DependencyArtifacts resolvedDependencies = (DependencyArtifacts) project
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        if (resolvedDependencies == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return resolvedDependencies;
    }

    public static Optional<DependencyArtifacts> getOptionalDependencyArtifacts(ReactorProject project) {
        DependencyArtifacts resolvedDependencies = (DependencyArtifacts) project
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        return Optional.ofNullable(resolvedDependencies);
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
    public static TargetPlatformConfiguration getTargetPlatformConfiguration(ReactorProject project)
            throws IllegalStateException {
        TargetPlatformConfiguration targetPlatformConfiguration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        if (targetPlatformConfiguration == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatformConfiguration;
    }

    /**
     * Returns the final target platform of the given project.
     */
    public static TargetPlatform getTargetPlatform(ReactorProject project) {
        TargetPlatform targetPlatform = getTargetPlatformIfAvailable(project);
        if (targetPlatform == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return targetPlatform;
    }

    /**
     * Returns the final target platform of the given project, or <code>null</code> if the target
     * platform is not available.
     * 
     * Projects with -Dtycho.targetPlatform use the legacy LocalDependencyResolver, which doesn't
     * provide the {@link TargetPlatform} interface.
     */
    public static TargetPlatform getTargetPlatformIfAvailable(ReactorProject project) {
        return (TargetPlatform) project.getContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY);
    }

    public static ExecutionEnvironmentConfiguration getExecutionEnvironmentConfiguration(ReactorProject project) {
        ExecutionEnvironmentConfiguration storedConfig = (ExecutionEnvironmentConfiguration) project
                .getContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION);
        if (storedConfig == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return storedConfig;
    }

    /**
     * Returns the (editable) list of {@link DependencySeed}s for the given project.
     */
    @SuppressWarnings("unchecked")
    public static List<DependencySeed> getDependencySeeds(ReactorProject project) {
        List<DependencySeed> dependencySeeds = (List<DependencySeed>) project
                .getContextValue(TychoConstants.CTX_DEPENDENCY_SEEDS);
        if (dependencySeeds == null) {
            dependencySeeds = new ArrayList<>();
            project.setContextValue(TychoConstants.CTX_DEPENDENCY_SEEDS, dependencySeeds);
        }
        return dependencySeeds;
    }

    /**
     * Returns the {@link DependencyArtifacts} instance associated with the given project and its
     * tests.
     * 
     * @param project
     *            a Tycho project
     * @return the resolved test dependencies of the given project; never <code>null</code>
     * @throws IllegalStateException
     *             if the given project does not have the resolved project dependencies stored
     */
    public static DependencyArtifacts getTestDependencyArtifacts(ReactorProject project) throws IllegalStateException {
        DependencyArtifacts resolvedDependencies = (DependencyArtifacts) project
                .getContextValue(TychoConstants.CTX_TEST_DEPENDENCY_ARTIFACTS);
        if (resolvedDependencies == null) {
            throw new IllegalStateException(TYCHO_NOT_CONFIGURED + project.toString());
        }
        return resolvedDependencies;
    }
}
