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
package org.eclipse.tycho.core.bnd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProjectManager;
import org.osgi.framework.Version;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(TychoConstants.PDE_BND)
public class BndClasspathContributor implements ClasspathContributor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TychoProjectManager projectManager;

    @Inject
    public BndClasspathContributor(TychoProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @Override
    public List<ClasspathEntry> getAdditionalClasspathEntries(MavenProject project, String scope) {

        Optional<Processor> bndTychoProject = projectManager.getBndTychoProject(project);
        if (bndTychoProject.isPresent()) {
            try (Processor processor = bndTychoProject.get()) {
                //See https://bnd.bndtools.org/instructions/classpath.html
                String classpath = processor.mergeProperties(Constants.CLASSPATH);
                if (classpath != null && !classpath.isBlank()) {
                    List<ClasspathEntry> additional = new ArrayList<>();
                    for (String file : classpath.split(",")) {
                        Matcher m = TychoConstants.PLATFORM_URL_PATTERN.matcher(file);
                        if (m.matches()) {
                            TargetPlatform targetPlatform = projectManager.getTargetPlatform(project)
                                    .orElseThrow(() -> new IllegalStateException("Project has no target platform"));
                            try {
                                ArtifactKey artifactKey = targetPlatform
                                        .resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, m.group(2), null);
                                File location = targetPlatform.getArtifactLocation(artifactKey);
                                additional.add(new BndClasspathEntry(location, artifactKey));
                            } catch (DependencyResolutionException | IllegalArtifactReferenceException e) {
                                throw new RuntimeException("can't resolve classpath entry " + file, e);
                            }
                        } else {
                            additional.add(new BndClasspathEntry(new File(project.getBasedir(), file.trim()), null));
                        }
                    }
                    return additional;
                }
            } catch (IOException e) {
                logger.warn("Can't determine additional classpath for " + project.getId(), e);
            }

        }
        return Collections.emptyList();
    }

    private static final class BndClasspathEntry implements ClasspathEntry {

        private File file;
        private ArtifactKey artifactKey;

        public BndClasspathEntry(File file, ArtifactKey artifactKey) {
            this.file = file;
            this.artifactKey = artifactKey == null ? new FileBasedKey(file) : artifactKey;
        }

        @Override
        public ArtifactKey getArtifactKey() {
            return artifactKey;
        }

        @Override
        public ReactorProject getMavenProject() {
            return null;
        }

        @Override
        public List<File> getLocations() {
            return List.of(file);
        }

        @Override
        public Collection<AccessRule> getAccessRules() {
            return null;
        }

    }

    private static final class FileBasedKey implements ArtifactKey {

        private File file;

        public FileBasedKey(File file) {
            this.file = file;
        }

        @Override
        public String getType() {
            return ArtifactType.TYPE_ECLIPSE_PLUGIN;
        }

        @Override
        public String getId() {
            return file.getAbsolutePath();
        }

        @Override
        public String getVersion() {
            return Version.emptyVersion.toString();
        }

    }
}
