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
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.eclipse.tycho.versions.pom.Profile;
import org.osgi.framework.Version;

@Component(role = VersionsEngine.class, instantiationStrategy = "per-lookup")
public class VersionsEngine {
    private static final String SUFFIX_QUALIFIER = ".qualifier";

    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    private static final String PACKAGING_POM = "pom";

    @Requirement
    private Logger logger;

    @Requirement(role = MetadataManipulator.class)
    private List<MetadataManipulator> manipulators;

    private Map<File, ProjectMetadata> projects = new LinkedHashMap<File, ProjectMetadata>();

    private Set<VersionChange> changes = new LinkedHashSet<VersionChange>();

    public void addBasedir(File basedir) throws IOException {
        // Unfold configuration inheritance

        if (projects.containsKey(basedir)) {
            // TODO test me
            return;
        }

        ProjectMetadata project = new ProjectMetadata(basedir);
        projects.put(basedir, project);

        MutablePomFile pom = MutablePomFile.read(new File(basedir, "pom.xml"));
        project.putMetadata(pom);

        String packaging = pom.getPackaging();
        if (PACKAGING_POM.equals(packaging)) {
            for (File child : getChildren(basedir, pom)) {
                addBasedir(child);
            }
        }
    }

    public void addVersionChange(String artifactId, String newVersion) throws IOException {
        ProjectMetadata project = getProject(artifactId);

        if (project == null) {
            // totally inappropriate. yuck.
            throw new IOException("Project with artifactId=" + artifactId + " cound not be found");
        }

        MutablePomFile pom = project.getMetadata(MutablePomFile.class);

        if (!newVersion.equals(pom.getEffectiveVersion())) {
            changes.add(new VersionChange(pom, newVersion));
        }
    }

    public void apply() throws IOException {
        // collecting secondary changes
        boolean newChanges = true;
        while (newChanges) {
            newChanges = false;
            for (VersionChange change : new ArrayList<VersionChange>(changes)) {
                for (ProjectMetadata project : projects.values()) {
                    for (MetadataManipulator manipulator : manipulators) {
                        newChanges |= manipulator.addMoreChanges(project, change, changes);
                    }
                }
            }
        }

        // make changes to the metadata
        for (ProjectMetadata project : projects.values()) {
            logger.info("Making changes in " + project.getBasedir().getCanonicalPath());
            for (VersionChange change : changes) {
                for (MetadataManipulator manipulator : manipulators) {
                    manipulator.applyChange(project, change, changes);
                }
            }
        }

        // write changes to the disk
        for (ProjectMetadata project : projects.values()) {
            for (MetadataManipulator manipulator : manipulators) {
                manipulator.writeMetadata(project);
            }
        }

    }

    private Set<File> getChildren(File basedir, MutablePomFile project) throws IOException {
        LinkedHashSet<File> children = new LinkedHashSet<File>();
        for (String module : project.getModules()) {
            children.add(new File(basedir, module).getCanonicalFile());
        }

        for (Profile profile : project.getProfiles()) {
            for (String module : profile.getModules()) {
                children.add(new File(basedir, module).getCanonicalFile());
            }
        }
        return children;
    }

    private ProjectMetadata getProject(String artifactId) {
        // TODO detect ambiguous artifactId
        for (ProjectMetadata project : projects.values()) {
            MutablePomFile pom = project.getMetadata(MutablePomFile.class);
            if (artifactId.equals(pom.getArtifactId())) {
                return project;
            }
        }
        return null;
    }

    public static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length()) + SUFFIX_QUALIFIER;
        }

        return version;
    }

    public static void assertIsOsgiVersion(String version) throws NumberFormatException, IllegalArgumentException,
            NullPointerException {
        new Version(version);
    }

    public static String toMavenVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_QUALIFIER)) {
            return version.substring(0, version.length() - SUFFIX_QUALIFIER.length()) + SUFFIX_SNAPSHOT;
        }

        return version;
    }

}
