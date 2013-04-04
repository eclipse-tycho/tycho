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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.versions.manipulation.PomManipulator;
import org.eclipse.tycho.versions.pom.GAV;
import org.eclipse.tycho.versions.pom.MutablePomFile;

/**
 * Applies direct and indirect version changes to a set of projects.
 * 
 * @TODO find more specific name that reflects what this class actually does.
 */
@Component(role = VersionsEngine.class, instantiationStrategy = "per-lookup")
public class VersionsEngine {

    private static class PropertyChange {
        final MutablePomFile pom;

        final String propertyName;

        String propertyValue;

        public PropertyChange(MutablePomFile pom, String propertyName, String propertyValue) {
            this.pom = pom;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }
    }

    @Requirement
    private Logger logger;

    @Requirement(role = MetadataManipulator.class)
    private List<MetadataManipulator> manipulators;

    @Requirement(hint = PomManipulator.HINT)
    private MetadataManipulator pomManipulator;

    private Collection<ProjectMetadata> projects;

    private Set<VersionChange> versionChanges = new LinkedHashSet<VersionChange>();

    private Set<PropertyChange> propertyChanges = new LinkedHashSet<PropertyChange>();

    public void setProjects(Collection<ProjectMetadata> projects) {
        this.projects = projects;
    }

    public void addVersionChange(String artifactId, String newVersion) throws IOException {
        MutablePomFile pom = getMutablePom(artifactId);

        if (!newVersion.equals(pom.getEffectiveVersion())) {
            addVersionChange(new VersionChange(pom, newVersion));
        }
    }

    private MutablePomFile getMutablePom(String artifactId) throws IOException {
        ProjectMetadata project = getProject(artifactId);

        if (project == null) {
            // totally inappropriate. yuck.
            throw new IOException("Project with artifactId=" + artifactId + " cound not be found");
        }

        return project.getMetadata(MutablePomFile.class);
    }

    public void addVersionChange(VersionChange change) {
        versionChanges.add(change);
    }

    public void apply() throws IOException {
        // collecting secondary changes
        boolean newChanges = true;
        while (newChanges) {
            newChanges = false;
            for (VersionChange change : new ArrayList<VersionChange>(versionChanges)) {
                for (ProjectMetadata project : projects) {
                    for (MetadataManipulator manipulator : manipulators) {
                        newChanges |= manipulator.addMoreChanges(project, change, versionChanges);
                    }
                }
            }
        }

        // validate version changes can be implemented
        List<String> errors = new ArrayList<String>();
        for (ProjectMetadata project : projects) {
            for (VersionChange change : versionChanges) {
                for (MetadataManipulator manipulator : manipulators) {
                    Collection<String> error = manipulator.validateChange(project, change);
                    if (error != null) {
                        errors.addAll(error);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalVersionChangeException(errors);
        }

        // make changes to the metadata
        for (ProjectMetadata project : projects) {
            logger.info("Making changes in " + project.getBasedir().getAbsolutePath());
            Set<VersionChange> applied = new HashSet<VersionChange>();

            MutablePomFile pom = project.getMetadata(MutablePomFile.class);
            GAV parent = pom != null ? pom.getParent() : null;

            // make changes to pom properties, assume project/version and project/parent/version are constants for now
            for (PropertyChange propertyChange : propertyChanges) {
                if (pom == propertyChange.pom) {
                    ((PomManipulator) pomManipulator).applyPropertyChange(pom, propertyChange.propertyName,
                            propertyChange.propertyValue);
                }
            }

            // apply change to pom <parent> first, this will avoid unnecessary addition of project/version element
            for (VersionChange change : versionChanges) {
                if (parent != null && PomManipulator.isGavEquals(parent, change)) {
                    applied.add(change);
                    applyChange(project, change);
                }
            }

            // apply all other changes
            for (VersionChange change : versionChanges) {
                if (!applied.contains(change)) {
                    applyChange(project, change);
                }
            }
        }

        // write changes to the disk
        for (ProjectMetadata project : projects) {
            for (MetadataManipulator manipulator : manipulators) {
                manipulator.writeMetadata(project);
            }
        }

    }

    private void applyChange(ProjectMetadata project, VersionChange change) {
        for (MetadataManipulator manipulator : manipulators) {
            manipulator.applyChange(project, change, versionChanges);
        }
    }

    private ProjectMetadata getProject(String artifactId) {
        // TODO detect ambiguous artifactId
        for (ProjectMetadata project : projects) {
            MutablePomFile pom = project.getMetadata(MutablePomFile.class);
            if (artifactId.equals(pom.getArtifactId())) {
                return project;
            }
        }
        return null;
    }

    public void addPropertyChange(String artifactId, String propertyName, String propertyValue) throws IOException {
        MutablePomFile pom = getMutablePom(artifactId);
        propertyChanges.add(new PropertyChange(pom, propertyName, propertyValue));
    }

}
