/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;

public class TychoProjectUtils {
    private static final String TYCHO_NOT_CONFIGURED = "Tycho build extension not configured for ";

    public static final String TYCHO_ENV_OSGI_WS = "tycho.env.osgi.ws";
    public static final String TYCHO_ENV_OSGI_OS = "tycho.env.osgi.os";
    public static final String TYCHO_ENV_OSGI_ARCH = "tycho.env.osgi.arch";

    public static Optional<DependencyArtifacts> getOptionalDependencyArtifacts(ReactorProject project) {
        DependencyArtifacts resolvedDependencies = (DependencyArtifacts) project
                .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
        return Optional.ofNullable(resolvedDependencies);
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
     * Computes the merged properties from maven session and the project
     * 
     * @param mavenProject
     * @param mavenSession
     * @return a (possibly cached) value, do not modify the object!
     */
    public static Properties getMergedProperties(MavenProject mavenProject, MavenSession mavenSession) {
        if (mavenSession == null) {
            //only temporary ...
            return computeMergedProperties(mavenProject, null);
        }
        return DefaultReactorProject.adapt(mavenProject, mavenSession).computeContextValue(
                ReactorProject.CTX_MERGED_PROPERTIES, () -> computeMergedProperties(mavenProject, mavenSession));
    }

    private static Properties computeMergedProperties(MavenProject mavenProject, MavenSession mavenSession) {
        Properties properties = new Properties();
        properties.putAll(mavenProject.getProperties());
        if (mavenSession != null) {
            properties.putAll(mavenSession.getSystemProperties()); // session wins
            properties.putAll(mavenSession.getUserProperties());
        }
        setTychoEnvironmentProperties(properties, mavenProject);
        return properties;
    }

    public static void setTychoEnvironmentProperties(Properties properties, MavenProject project) {
        String arch = PlatformPropertiesUtils.getArch(properties);
        String os = PlatformPropertiesUtils.getOS(properties);
        String ws = PlatformPropertiesUtils.getWS(properties);
        project.getProperties().put(TYCHO_ENV_OSGI_WS, ws);
        project.getProperties().put(TYCHO_ENV_OSGI_OS, os);
        project.getProperties().put(TYCHO_ENV_OSGI_ARCH, arch);
    }
}
