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
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;

@Component(role = MetadataManipulator.class, hint = "eclipse-feature")
public class FeatureXmlManipulator extends AbstractMetadataManipulator {
    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (isFeature(project)) {
            Feature feature = getFeatureXml(project);
            if (isFeature(change.getProject().getPackaging())) {
                if (change.getArtifactId().equals(feature.getId()) && change.getVersion().equals(feature.getVersion())) {
                    logger.info("  feature.xml//feature/@version: " + change.getVersion() + " => "
                            + change.getNewVersion());
                    feature.setVersion(change.getNewVersion());
                }
                // could be included feature
                changeIncludedFeatures(change, feature);
            } else if (isBundle(change.getProject())) {
                changeIncludedPlugins(change, feature);
            }
        }
    }

    private void changeIncludedFeatures(VersionChange change, Feature feature) {
        for (FeatureRef ref : feature.getIncludedFeatures()) {
            if (change.getArtifactId().equals(ref.getId()) && change.getVersion().equals(ref.getVersion())) {
                logger.info("  feature.xml//feature/includes/@id='" + ref.getId() + "'/@version: "
                        + change.getVersion() + " => " + change.getNewVersion());
                ref.setVersion(change.getNewVersion());
            }
        }
    }

    private void changeIncludedPlugins(VersionChange change, Feature feature) {
        for (PluginRef plugin : feature.getPlugins()) {
            if (change.getArtifactId().equals(plugin.getId()) && change.getVersion().equals(plugin.getVersion())) {
                logger.info("  feature.xml//feature/plugin/@id='" + plugin.getId() + "'/@version: "
                        + change.getVersion() + " => " + change.getNewVersion());
                plugin.setVersion(change.getNewVersion());
            }
        }
    }

    private Feature getFeatureXml(ProjectMetadata project) {
        Feature feature = project.getMetadata(Feature.class);
        if (feature == null) {
            File file = new File(project.getBasedir(), Feature.FEATURE_XML);
            try {
                feature = Feature.read(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read feature descriptor" + file, e);
            }
            project.putMetadata(feature);
        }
        return feature;
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        Feature feature = project.getMetadata(Feature.class);
        if (feature != null) {
            Feature.write(feature, new File(project.getBasedir(), Feature.FEATURE_XML));
        }
    }
}
