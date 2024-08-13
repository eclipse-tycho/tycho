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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;

import de.pdark.decentxml.Element;

@Named
public class MavenLocationUpdater {

    @Inject
    private MavenDependenciesResolver resolver;

    public boolean update(Element mavenLocation, UpdateTargetMojo context)
            throws VersionRangeResolutionException, ArtifactResolutionException {
        boolean changed = false;
        Element dependencies = mavenLocation.getChild("dependencies");
        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency")) {
                Dependency mavenDependency = getDependency(dependency);
                String oldVersion = mavenDependency.getVersion();
                if (!context.isUpdateMajorVersion()) {
                    try {
                        String[] strings = oldVersion.split("\\.");
                        mavenDependency.setVersion("[," + (Integer.parseInt(strings[0]) + 1) + "-alpha)");
                    } catch (RuntimeException e) {
                        context.getLog().warn("Can't check for update of " + mavenDependency
                                + " because the version format is not parseable: " + e);
                        continue;
                    }
                }
                Artifact newArtifactVersion = resolver.resolveHighestVersion(context.getProject(),
                        context.getMavenSession(), mavenDependency);
                if (newArtifactVersion == null) {
                    continue;
                }
                String newVersion = newArtifactVersion.getVersion();
                if (newVersion.equals(oldVersion)) {
                    context.getLog().debug(mavenDependency + " is already up-to date");
                } else {
                    changed = true;
                    UpdateTargetMojo.setElementValue("version", newVersion, dependency);
                    context.getLog().info("update " + mavenDependency + " to version " + newVersion);
                }
            }
        }
        return changed;
    }

    private static Dependency getDependency(Element dependency) {
        Dependency mavenDependency = new Dependency();
        mavenDependency.setGroupId(UpdateTargetMojo.getElementValue("groupId", dependency));
        mavenDependency.setArtifactId(UpdateTargetMojo.getElementValue("artifactId", dependency));
        mavenDependency.setVersion(UpdateTargetMojo.getElementValue("version", dependency));
        mavenDependency.setType(UpdateTargetMojo.getElementValue("type", dependency));
        mavenDependency.setClassifier(UpdateTargetMojo.getElementValue("classifier", dependency));
        return mavenDependency;
    }

}
