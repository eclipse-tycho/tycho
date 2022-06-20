/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #460 - Delay classpath resolution to the compile phase 
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.List;
import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;

public abstract class AbstractTychoProject extends AbstractLogEnabled implements TychoProject {

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project) {
        return TychoProjectUtils.getDependencyArtifacts(project);
    }

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project, TargetEnvironment environment) {
        DependencyArtifacts platform = getDependencyArtifacts(project);

        if (environment != null && platform instanceof MultiEnvironmentDependencyArtifacts) {
            platform = ((MultiEnvironmentDependencyArtifacts) platform).getPlatform(environment);

            if (platform == null) {
                throw new IllegalStateException("Unsupported runtime environment " + environment.toString()
                        + " for project " + project.toString());
            }
        }

        return platform;
    }

    public void setDependencyArtifacts(MavenSession session, ReactorProject project,
            DependencyArtifacts dependencyArtifacts) {
        project.setContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS, dependencyArtifacts);
    }

    public void setTestDependencyArtifacts(MavenSession session, ReactorProject project,
            DependencyArtifacts dependencyArtifacts) {
        project.setContextValue(TychoConstants.CTX_TEST_DEPENDENCY_ARTIFACTS, dependencyArtifacts);
    }

    public void setupProject(MavenSession session, MavenProject project) {
        // do nothing by default
    }

    protected TargetEnvironment[] getEnvironments(ReactorProject project, TargetEnvironment environment) {
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

    @Override
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project) {
        return null;
    }

    public void readExecutionEnvironmentConfiguration(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        TargetPlatformConfiguration tpConfiguration = TychoProjectUtils.getTargetPlatformConfiguration(project);

        String configuredForcedProfile = tpConfiguration.getExecutionEnvironment();
        if (configuredForcedProfile != null) {
            sink.overrideProfileConfiguration(configuredForcedProfile,
                    "target-platform-configuration <executionEnvironment>");
        } else {
            tpConfiguration.getTargets().stream() //
                    .map(TargetDefinition::getTargetEE) //
                    .filter(Objects::nonNull) //
                    .findFirst() //
                    .ifPresent(profile -> sink.overrideProfileConfiguration(profile,
                            "first targetJRE from referenced target-definition files"));
        }

        String configuredDefaultProfile = tpConfiguration.getExecutionEnvironmentDefault();
        if (configuredDefaultProfile != null) {
            sink.setProfileConfiguration(configuredDefaultProfile,
                    "target-platform-configuration <executionEnvironmentDefault>");
        }
    }

}
