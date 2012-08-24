/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

public abstract class AbstractTychoProject extends AbstractLogEnabled implements TychoProject {

    public DependencyArtifacts getDependencyArtifacts(MavenProject project) {
        return TychoProjectUtils.getDependencyArtifacts(project);
    }

    public DependencyArtifacts getDependencyArtifacts(MavenProject project, TargetEnvironment environment) {
        DependencyArtifacts platform = getDependencyArtifacts(project);

        if (environment != null && platform instanceof MultiEnvironmentTargetPlatform) {
            platform = ((MultiEnvironmentTargetPlatform) platform).getPlatform(environment);

            if (platform == null) {
                throw new IllegalStateException("Unsupported runtime environment " + environment.toString()
                        + " for project " + project.toString());
            }
        }

        return platform;
    }

    public void setDependencyArtifacts(MavenSession session, MavenProject project,
            DependencyArtifacts dependencyArtifacts) {
        project.setContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS, dependencyArtifacts);
    }

    public void setupProject(MavenSession session, MavenProject project) {
        // do nothing by default
    }

    /**
     * @deprecated Only needed for {@link LocalTargetPlatformResolver}; p2 resolver checks
     *             consistency itself
     */
    @Deprecated
    public void checkForMissingDependencies(MavenProject project) {
    }

    public void resolveClassPath(MavenSession session, MavenProject project) {
        // do nothing by default
    }

    protected TargetEnvironment[] getEnvironments(MavenProject project, TargetEnvironment environment) {
        if (environment != null) {
            return new TargetEnvironment[] { environment };
        }

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        if (configuration.isImplicitTargetEnvironment()) {
            return null; // any
        }

        // all specified
        List<TargetEnvironment> environments = configuration.getEnvironments();
        return environments.toArray(new TargetEnvironment[environments.size()]);
    }

    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project) {
        return null;
    }

    public ExecutionEnvironment getExecutionEnvironment(MavenProject project) {
        TargetPlatformConfiguration tpConfiguration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        String profile = tpConfiguration.getExecutionEnvironment();

        if (profile == null) {
            profile = tpConfiguration.getExecutionEnvironmentDefault();
        }

        if (profile == null) {
            // TODO 387796 set global default here?
            return null;
        }

        return ExecutionEnvironmentUtils.getExecutionEnvironment(profile);
    }

    public TargetPlatform getTargetPlatform(MavenProject project) {
        return (TargetPlatform) project.getContextValue(TychoConstants.CTX_TARGET_PLATFORM);
    }

    public void setTargetPlatform(MavenProject project, TargetPlatform targetPlatform) {
        project.setContextValue(TychoConstants.CTX_TARGET_PLATFORM, targetPlatform);
    }
}
