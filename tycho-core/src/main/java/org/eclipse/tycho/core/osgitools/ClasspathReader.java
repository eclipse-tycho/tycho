/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.model.classpath.ClasspathParser;
import org.eclipse.tycho.model.classpath.JUnitBundle;
import org.eclipse.tycho.model.classpath.ProjectClasspathEntry;
import org.eclipse.tycho.model.project.EclipseProject;

@Component(role = ClasspathReader.class)
public class ClasspathReader implements Disposable {

    private Map<String, Collection<ProjectClasspathEntry>> cache = new ConcurrentHashMap<>();

    @Requirement
    Logger logger;

    @Requirement
    TychoProjectManager projectManager;

    @Override
    public void dispose() {
        cache.clear();
    }

    public Collection<ProjectClasspathEntry> parse(File basedir) throws IOException {
        Optional<EclipseProject> eclipseProject = projectManager.getEclipseProject(basedir);
        Path resolvedClasspath = eclipseProject.map(project -> project.getFile(ClasspathParser.CLASSPATH_FILENAME))
                .orElse(basedir.toPath().resolve(ClasspathParser.CLASSPATH_FILENAME));

        return cache.computeIfAbsent(resolvedClasspath.normalize().toString(), f -> {
            File resolvedClasspathFile = resolvedClasspath.toFile();
            try {
                if (eclipseProject.isPresent()) {
                    return ClasspathParser.parse(resolvedClasspathFile, eclipseProject.get());
                } else {
                    return ClasspathParser.parse(resolvedClasspathFile);
                }
            } catch (IOException e) {
                logger.warn("Can't read classpath from " + basedir);
                return Collections.emptyList();
            }
        });
    }

    public static Collection<MavenArtifactKey> asMaven(Collection<JUnitBundle> artifacts) {
        return artifacts.stream().map(junit -> toMaven(junit)).toList();
    }

    public static MavenArtifactKey toMaven(JUnitBundle junit) {
        return MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT, junit.getBundleName(), junit.getVersionRange(),
                junit.getMavenGroupId(), junit.getMavenArtifactId());
    }

}
