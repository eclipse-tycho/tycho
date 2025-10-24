/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - updateVersionRangeMatchingBounds 
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.versions.manipulation.PomManipulator;
import org.eclipse.tycho.versions.pom.PomFile;

/**
 * Applies direct and indirect version changes to a set of projects.
 * 
 * @TODO find more specific name that reflects what this class actually does.
 * 
 */
@Named
public class VersionsEngine {

    private static class PropertyChange {
        final PomFile pom;

        final String propertyName;

        String propertyValue;

        public PropertyChange(PomFile pom, String propertyName, String propertyValue) {
            this.pom = pom;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }
    }

    @Inject
    private Logger logger;

    @Inject
    private List<MetadataManipulator> manipulators;

    @Inject
    @Named(PomManipulator.HINT)
    private PomManipulator pomManipulator;

    private Collection<ProjectMetadata> projects;

    private Set<PomVersionChange> originalVersionChanges = new LinkedHashSet<>();

    private Set<PropertyChange> propertyChanges = new LinkedHashSet<>();

    private boolean updateVersionRangeMatchingBounds;

    public boolean isUpdateVersionRangeMatchingBounds() {
        return updateVersionRangeMatchingBounds;
    }

    public void setUpdateVersionRangeMatchingBounds(boolean updateVersionRangeMatchingBounds) {
        this.updateVersionRangeMatchingBounds = updateVersionRangeMatchingBounds;
    }

    public void setProjects(Collection<ProjectMetadata> projects) {
        this.projects = projects;
    }

    public void addVersionChange(String artifactId, String newVersion) throws IOException {
        PomFile pom = getMutablePom(artifactId);

        if (!newVersion.equals(pom.getVersion())) {
            addVersionChange(new PomVersionChange(pom, newVersion));
        }
    }

    public PomFile getMutablePom(String artifactId) throws IOException {
        ProjectMetadata project = getProject(artifactId);

        if (project == null) {
            // totally inappropriate. yuck.
            throw new IOException("Project with artifactId=" + artifactId + " could not be found");
        }

        return project.getMetadata(PomFile.class);
    }

    public void addVersionChange(PomVersionChange change) {
        originalVersionChanges.add(change);
    }

    public void reset() {
        originalVersionChanges.clear();
        propertyChanges.clear();
        updateVersionRangeMatchingBounds = false;
        projects = null;
    }

    public void apply() throws IOException {

        VersionChangesDescriptor versionChangeContext = new VersionChangesDescriptor(originalVersionChanges,
                new DefaultVersionRangeUpdateStrategy(isUpdateVersionRangeMatchingBounds()), projects);

        // collecting secondary changes
        boolean newChanges = true;
        while (newChanges) {
            newChanges = false;
            for (ProjectMetadata project : projects) {
                for (MetadataManipulator manipulator : manipulators) {
                    newChanges |= manipulator.addMoreChanges(project, versionChangeContext);
                }
            }
        }

        // validate version changes can be implemented
        List<String> errors = new ArrayList<>();
        for (ProjectMetadata project : projects) {
            for (MetadataManipulator manipulator : manipulators) {
                Collection<String> error = manipulator.validateChanges(project, versionChangeContext);
                if (error != null) {
                    errors.addAll(error);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalVersionChangeException(errors);
        }

        // make changes to the metadata
        for (ProjectMetadata project : projects) {
            logger.info("Making changes in " + project.getBasedir().getAbsolutePath());

            PomFile pom = project.getMetadata(PomFile.class);

            // make changes to pom properties, assume project/version and project/parent/version are constants for now
            // TODO property changes should be added as a new type of change in VersionChangeDescriptors
            for (PropertyChange propertyChange : propertyChanges) {
                if (pom == propertyChange.pom) {
                    pomManipulator.applyPropertyChange(project.getPomFile().getName(), pom,
                            propertyChange.propertyName, propertyChange.propertyValue);
                }
            }

            // apply changes
            for (MetadataManipulator manipulator : manipulators) {
                manipulator.applyChanges(project, versionChangeContext);
            }
        }

        // write changes to the disk
        for (ProjectMetadata project : projects) {
            for (MetadataManipulator manipulator : manipulators) {
                manipulator.writeMetadata(project);
            }
        }

    }

    private ProjectMetadata getProject(String artifactId) {
        // TODO detect ambiguous artifactId
        for (ProjectMetadata project : projects) {
            PomFile pom = project.getMetadata(PomFile.class);
            if (artifactId.equals(pom.getArtifactId())) {
                return project;
            }
        }
        return null;
    }

    public void addPropertyChange(String artifactId, String propertyName, String propertyValue) throws IOException {
        PomFile pom = getMutablePom(artifactId);
        propertyChanges.add(new PropertyChange(pom, propertyName, propertyValue));
    }

}
