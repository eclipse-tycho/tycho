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
package org.eclipse.tycho.versionbump;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.Wagon;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.DefaultVersionsHelper;
import org.codehaus.mojo.versions.api.VersionRetrievalException;
import org.codehaus.mojo.versions.api.VersionsHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.tycho.TychoConstants;

import de.pdark.decentxml.Element;

/**
 * Supports updating of maven target locations
 */
@Named
public class MavenLocationUpdater {

    @Inject
    protected ArtifactHandlerManager artifactHandlerManager;

    @Inject
    protected RepositorySystem repositorySystem;
    @Inject
    protected Map<String, Wagon> wagonMap;

    boolean update(Element mavenLocation, UpdateTargetMojo context) throws VersionRangeResolutionException,
            ArtifactResolutionException, MojoExecutionException, VersionRetrievalException {
        VersionsHelper helper = getHelper(context);
        boolean changed = false;
        Element dependencies = mavenLocation.getChild("dependencies");
        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency")) {
                Dependency mavenDependency = getDependency(dependency);
                Artifact dependencyArtifact = helper.createDependencyArtifact(mavenDependency);
                ArtifactVersions versions = helper.lookupArtifactVersions(dependencyArtifact, false);
                ArtifactVersion updateVersion = context.getSegments()
                        .map(seg -> versions.getNewestUpdateWithinSegment(Optional.of(seg), false))
                        .filter(Objects::nonNull).findFirst().orElse(null);
                if (updateVersion != null) {
                    String oldVersion = mavenDependency.getVersion();
                    String newVersion = updateVersion.toString();
                    if (newVersion.equals(oldVersion)) {
                        context.getLog().debug(mavenDependency + " is already up-to date");
                    } else {
                        changed = true;
                        UpdateTargetMojo.setElementValue("version", newVersion, dependency);
                        context.getLog().info("update " + mavenDependency + " to version " + newVersion);
                    }
                }
            }
        }
        return changed;
    }

    VersionsHelper getHelper(UpdateTargetMojo context) throws MojoExecutionException {
        return new DefaultVersionsHelper.Builder().withArtifactHandlerManager(artifactHandlerManager)
                .withRepositorySystem(repositorySystem).withWagonMap(wagonMap).withServerId("serverId")
                .withRulesUri(context.getMavenRulesUri()).withRuleSet(context.getMavenRuleSet())
                .withIgnoredVersions(context.getMavenIgnoredVersions()).withLog(context.getLog())
                .withMavenSession(context.getMavenSession()).withMojoExecution(context.getMojoExecution()).build();
    }

    private static Dependency getDependency(Element dependency) {
        Dependency mavenDependency = new Dependency();
        mavenDependency.setGroupId(UpdateTargetMojo.getElementValue("groupId", dependency));
        mavenDependency.setArtifactId(UpdateTargetMojo.getElementValue("artifactId", dependency));
        mavenDependency.setVersion(UpdateTargetMojo.getElementValue("version", dependency));
        mavenDependency.setType(UpdateTargetMojo.getElementValue("type", dependency));
        mavenDependency.setClassifier(UpdateTargetMojo.getElementValue("classifier", dependency));
        if (mavenDependency.getType() == null) {
            mavenDependency.setType(TychoConstants.JAR_EXTENSION);
        }
        return mavenDependency;
    }

}
