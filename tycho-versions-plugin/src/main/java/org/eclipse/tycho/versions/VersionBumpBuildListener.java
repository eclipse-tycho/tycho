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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.util.Optional;

import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.build.BuildListener;
import org.eclipse.tycho.core.exceptions.VersionBumpRequiredException;
import org.eclipse.tycho.helper.ProjectHelper;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.engine.VersionsEngine;
import org.eclipse.tycho.versions.pom.PomFile;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

// TODO: this was a singleton component but none of the 3 dependencies are singletons!
@Named("version-bump")
public class VersionBumpBuildListener implements BuildListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProjectMetadataReader metadataReader;
    private final VersionsEngine engine;
    private final ProjectHelper projectHelper;

    @Inject
    public VersionBumpBuildListener(ProjectMetadataReader metadataReader,
                                    VersionsEngine engine,
                                    ProjectHelper projectHelper) {
        this.metadataReader = metadataReader;
        this.engine = engine;
        this.projectHelper = projectHelper;
    }

    @Override
    public void buildStarted(MavenSession session) {
        //nothing specifically to do
    }

    @Override
    public void buildEnded(MavenSession session) {
        MavenExecutionResult result = session.getResult();
        int bumped = 0;
        int skipped = 0;
        for (MavenProject project : session.getProjects()) {
            BuildSummary buildSummary = result.getBuildSummary(project);
            if (buildSummary == null) {
                skipped++;
                continue;
            }
            if (buildSummary instanceof BuildFailure failure) {
                VersionBumpRequiredException vbe = getVersionBumpRequiredException(failure.getCause());
                if (vbe == null) {
                    continue;
                }
                if (hasBumpExecution(project, session)) {
                    try {
                        metadataReader.reset();
                        engine.reset();
                        PomFile pomFile = metadataReader.addBasedir(project.getBasedir(), false);
                        if (pomFile != null) {
                            String currentVersion = pomFile.getVersion();
                            Optional<Version> suggestedVersion = vbe.getSuggestedVersion();
                            String newVersion = suggestedVersion.map(String::valueOf)
                                    .orElseGet(() -> Versions.incrementVersion(currentVersion,
                                            VersionBumpMojo.getIncrement(session, project, projectHelper)));
                            boolean isSnapshot = currentVersion.endsWith(TychoConstants.SUFFIX_SNAPSHOT);
                            if (isSnapshot && !newVersion.endsWith(TychoConstants.SUFFIX_SNAPSHOT)) {
                                newVersion += TychoConstants.SUFFIX_SNAPSHOT;
                            }
                            logger.info(project.getId() + " requires a version bump from " + currentVersion + " => "
                                    + newVersion);
                            engine.setProjects(metadataReader.getProjects());
                            engine.addVersionChange(pomFile.getArtifactId(), newVersion);
                            engine.apply();
                            bumped++;
                        }
                    } catch (Exception e) {
                        logger.warn("Can't bump versions for project " + project.getId() + ": " + e);
                    }
                }
            }
        }
        if (bumped > 0) {
            logger.warn(bumped + " project version(s) where bumped, rerun the build to take the changes into account!");
            if (skipped > 0) {
                logger.warn(skipped + " project(s) where skipped, more version bumps might be required.");
            }
        }
    }

    private boolean hasBumpExecution(MavenProject project, MavenSession mavenSession) {
        return projectHelper.hasPluginExecution(VersionBumpMojo.GROUP_ID, VersionBumpMojo.ARTIFACT_ID,
                VersionBumpMojo.NAME, project, mavenSession);
    }

    private VersionBumpRequiredException getVersionBumpRequiredException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof VersionBumpRequiredException vbe) {
            return vbe;
        }
        return getVersionBumpRequiredException(throwable.getCause());
    }

}
