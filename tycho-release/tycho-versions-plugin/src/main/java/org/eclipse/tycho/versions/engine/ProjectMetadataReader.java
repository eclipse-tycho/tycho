/*******************************************************************************
 * Copyright (c) 2011, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.eclipse.tycho.versions.pom.Profile;

@Component(role = ProjectMetadataReader.class, instantiationStrategy = "per-lookup")
public class ProjectMetadataReader {
    private static final String PACKAGING_POM = "pom";

    @Requirement
    private Logger log;

    private Map<File, ProjectMetadata> projects = new LinkedHashMap<File, ProjectMetadata>();

    public void addBasedir(File basedir) throws IOException {
        // Unfold configuration inheritance

        if (!basedir.exists()) {
            log.info("Project does not exist at " + basedir);
            return;
        }

        // normalize basedir to allow modules that explicitly point at pom.xml file

        if (basedir.isFile()) {
            if (!MutablePomFile.POM_XML.equals(basedir.getName())) {
                // TODO support custom pom.xml file names
                log.info("Custom pom.xml file name is not supported at " + basedir);
                return;
            }
            basedir = basedir.getParentFile();
        }

        if (projects.containsKey(basedir)) {
            // TODO test me
            return;
        }

        ProjectMetadata project = new ProjectMetadata(basedir);
        projects.put(basedir, project);

        MutablePomFile pom = MutablePomFile.read(new File(basedir, MutablePomFile.POM_XML));
        project.putMetadata(pom);

        String packaging = pom.getPackaging();
        if (PACKAGING_POM.equals(packaging)) {
            for (File child : getChildren(basedir, pom)) {
                addBasedir(child);
            }
        }
    }

    private Set<File> getChildren(File basedir, MutablePomFile project) throws IOException {
        LinkedHashSet<File> children = new LinkedHashSet<File>();
        for (String module : project.getModules()) {
            children.add(canonify(new File(basedir, module)));
        }

        for (Profile profile : project.getProfiles()) {
            for (String module : profile.getModules()) {
                children.add(canonify(new File(basedir, module)));
            }
        }
        return children;
    }

    public Collection<ProjectMetadata> getProjects() {
        return projects.values();
    }

    private File canonify(File file) {
        return new File(file.toURI().normalize());
    }

}
