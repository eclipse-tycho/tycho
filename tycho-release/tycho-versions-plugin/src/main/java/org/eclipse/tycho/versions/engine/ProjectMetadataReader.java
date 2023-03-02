/*******************************************************************************
 * Copyright (c) 2011, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *    Christoph Läubrich - Bug 550313 - tycho-versions-plugin uses hard-coded polyglot file 
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.versions.pom.GAV;
import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.pom.Profile;

@Component(role = ProjectMetadataReader.class, instantiationStrategy = "per-lookup")
public class ProjectMetadataReader {
    private static final String PACKAGING_POM = "pom";

    @Requirement
    private Logger log;
    @Requirement
    private PlexusContainer container;

    private Map<File, ProjectMetadata> projects = new LinkedHashMap<>();

    public void reset() {
        projects.clear();
    }

    public PomFile addBasedir(File basedir, boolean recursive) throws IOException {
        // Unfold configuration inheritance

        if (!basedir.exists()) {
            log.info("Project does not exist at " + basedir);
            return null;
        }
        List<ModelProcessor> modelprocessors;
        try {
            modelprocessors = container.lookupList(ModelProcessor.class);
        } catch (ComponentLookupException e) {
            throw new IOException("can't lookup ModelProcessors");
        }
        // normalize basedir to allow modules that explicitly point at pom.xml file

        File pomFile = null;
        if (basedir.isFile()) {
            pomFile = basedir;
            basedir = basedir.getParentFile();
        }

        if (projects.containsKey(basedir)) {
            return null;
        }

        ProjectMetadata project = new ProjectMetadata(basedir);
        for (ModelProcessor modelProcessor : modelprocessors) {
            File locatePom = modelProcessor.locatePom(basedir);
            if (basedir.exists()) {
                pomFile = locatePom;
                break;
            }
        }
        if (pomFile == null || !pomFile.exists() || pomFile.length() == 0) {
            log.warn("No pom file found at " + basedir);
            return null;
        }
        projects.put(basedir, project);
        PomFile pom = PomFile.read(pomFile, PomFile.POM_XML.equals(pomFile.getName()));
        project.putMetadata(pom);

        if (recursive) {
            if (PACKAGING_POM.equals(pom.getPackaging())) {
                for (File child : getChildren(basedir, pom)) {
                    addBasedir(child, recursive);
                }
            }
            GAV parent = pom.getParent();
            if (parent != null) {
                String relativePath = parent.getRelativePath();
                if (relativePath == null) {
                    relativePath = "../pom.xml";
                }
                //this case is required if a child module includes another parent that in fact then uses the parent from the tree
                //if we don't add this as well, the version update miss the indirectly referenced parent to be updated
                File parentProjectPath = new File(basedir, relativePath);
                if (parentProjectPath.exists()) {
                    addBasedir(canonify(parentProjectPath), recursive);
                }
            }
        }
        return pom;
    }

    private Set<File> getChildren(File basedir, PomFile project) throws IOException {
        LinkedHashSet<File> children = new LinkedHashSet<>();
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
