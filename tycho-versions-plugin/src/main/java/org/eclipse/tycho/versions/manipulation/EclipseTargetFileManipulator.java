/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.TargetFiles;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.PomFile;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

@Named("eclipse-target-files")
@Singleton
public class EclipseTargetFileManipulator extends AbstractMetadataManipulator {

    private static final String SEQUENCE_NUMBER_ATTRIBUTE = "sequenceNumber";
    private static final String MVN_URL = "mvn";
    private static final String MVN_URL_PREFIX = MVN_URL + ":";
    private static final String TARGET_TYPE = "Target";
    private static final String TARGET_TYPE_URI_ATTRIBUTE = "uri";

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseTargetProject(project)) {
            for (PomVersionChange change : versionChangeContext.getVersionChanges()) {
                for (Entry<File, Document> entry : getTargets(project).entrySet()) {
                    applyChanges(change, entry.getValue(), entry.getKey().getName());
                }
            }
        }
    }

    private void applyChanges(PomVersionChange change, Document document, String fileName) {
        Element dom = document.getRootElement();
        boolean changed = false;
        for (Element locations : dom.getChildren("locations")) {
            List<Element> children = locations.getChildren("location");
            for (int j = 0; j < children.size(); j++) {
                Element location = children.get(j);
                String locationType = location.getAttributeValue("type");
                //TODO probably also update maven target locations?
                if (TARGET_TYPE.equals(locationType)) {
                    String uri = location.getAttributeValue(TARGET_TYPE_URI_ATTRIBUTE);
                    if (uri.startsWith(MVN_URL_PREFIX)) {
                        String[] coordinates = uri.substring(MVN_URL_PREFIX.length()).split(":");
                        if (coordinates.length < 3) {
                            continue;
                        }
                        String groupId = coordinates[0];
                        String artifactId = coordinates[1];
                        String version = coordinates[2];
                        if (groupId.equals(change.getGroupId()) && artifactId.equals(change.getArtifactId())
                                && Versions.isVersionEquals(version, change.getVersion())) {
                            Builder<String> builder = Stream.builder();
                            builder.add(MVN_URL);
                            builder.add(groupId);
                            builder.add(artifactId);
                            builder.add(Versions.toMavenVersion(change.getNewVersion()));
                            for (int i = 3; i < coordinates.length; i++) {
                                builder.add(coordinates[i]);
                            }
                            String newUri = builder.build().collect(Collectors.joining(":"));
                            logger.info("  " + fileName + "//target/locations/location[" + j + "]/@"
                                    + TARGET_TYPE_URI_ATTRIBUTE + ": " + uri + " => " + newUri);
                            changed = true;
                            location.setAttribute(TARGET_TYPE_URI_ATTRIBUTE, newUri);
                        }
                    }
                }
            }
        }
        if (changed) {
            try {
                int sequenceNumber = Integer.parseInt(dom.getAttributeValue(SEQUENCE_NUMBER_ATTRIBUTE));
                int nextSequenceNumber = sequenceNumber + 1;
                dom.setAttribute(SEQUENCE_NUMBER_ATTRIBUTE, String.valueOf(nextSequenceNumber));
                logger.info("  " + fileName + "//target/@" + SEQUENCE_NUMBER_ATTRIBUTE + ": " + sequenceNumber + " => "
                        + nextSequenceNumber);
            } catch (NumberFormatException e) {
                //skip then..
            }

        }

    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseTargetProject(project)) {
            //nothing to validate here..
        }
        return null;
    }

    private boolean isEclipseTargetProject(ProjectMetadata project) {
        return PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(project.getMetadata(PomFile.class).getPackaging());
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        TargetFiles targetFiles = project.getMetadata(TargetFiles.class);
        if (targetFiles != null) {
            targetFiles.write();
        }
    }

    private Map<File, Document> getTargets(ProjectMetadata project) {
        TargetFiles targets = project.getMetadata(TargetFiles.class);
        if (targets == null) {
            targets = new TargetFiles();
            File[] targetFiles = TargetDefinitionFile.listTargetFiles(project.getBasedir());
            for (File targetFile : targetFiles) {
                try {
                    targets.addTargetFile(targetFile);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not read target file " + targetFile, e);
                }
            }
            project.putMetadata(targets);
        }
        return targets.getTargets();
    }

}
