/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.targetplatform.TargetDefinition.ImplicitDependency;

@Named("target-platform")
@Singleton
public class TargetPlatformClasspathContributor implements ClasspathContributor {

    @Inject
    private Logger logger;

    @Inject
    private TychoProjectManager projectManager;

    @Override
    public List<ClasspathEntry> getAdditionalClasspathEntries(MavenProject project, String scope) {

        TargetPlatform platform = projectManager.getTargetPlatform(project).orElse(null);
        if (platform == null) {
            return List.of();
        }
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        List<ImplicitDependency> dependencies = configuration.getTargets().stream()
                .flatMap(tdf -> tdf.implicitDependencies()).distinct().toList();
        return dependencies.stream().map(dependency -> getClasspathEntry(platform, dependency)).filter(Objects::nonNull)
                .toList();
    }

    private ClasspathEntry getClasspathEntry(TargetPlatform targetPlatform, ImplicitDependency dependency) {
        try {
            ArtifactKey key = targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, dependency.getId(),
                    null);

            return new TargetPlatformClasspathEntry(targetPlatform, key);
        } catch (Exception e) {
            logger.warn("Can't resolve ImplicitDependency with id " + dependency.getId(), e);
            return null;
        }
    }

    private static final class TargetPlatformClasspathEntry implements ClasspathEntry {
        private final TargetPlatform targetPlatform;
        private final ArtifactKey key;
        private List<File> files;

        private TargetPlatformClasspathEntry(TargetPlatform targetPlatform, ArtifactKey key) {
            this.targetPlatform = targetPlatform;
            this.key = key;
        }

        @Override
        public ReactorProject getMavenProject() {
            return null;
        }

        @Override
        public synchronized List<File> getLocations() {
            if (files == null) {
                File file = targetPlatform.getArtifactLocation(getArtifactKey());
                if (file == null) {
                    files = List.of();
                } else {
                    files = List.of(file);
                }
            }
            return files;
        }

        @Override
        public ArtifactKey getArtifactKey() {
            return key;
        }

        @Override
        public Collection<AccessRule> getAccessRules() {
            return null;
        }

        @Override
        public String toString() {
            return "TargetPlatformClasspathEntry[" + key + "]";
        }
    }

}
