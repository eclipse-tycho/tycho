/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;

@Component(role = MetadataManipulator.class, hint = "eclipse-feature")
public class FeatureXmlManipulator extends AbstractMetadataManipulator {

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isFeature(project)) {
            Feature feature = getFeatureXml(project);
            for (VersionChange change : versionChangeContext.getVersionChanges()) {
                if (isFeature(change.getProject().getPackaging())) {
                    if (change.getArtifactId().equals(feature.getId())
                            && change.getVersion().equals(feature.getVersion())) {
                        logger.info("  feature.xml//feature/@version: " + change.getVersion() + " => "
                                + change.getNewVersion());
                        feature.setVersion(change.getNewVersion());
                    }
                    changeLicenseFeature(change, feature);
                    // could be included feature
                    changeIncludedFeatures(change, feature);
                } else if (isBundle(change.getProject())) {
                    changeIncludedPlugins(change, feature);
                }
            }
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isFeature(project)) {
            Feature feature = getFeatureXml(project);
            for (VersionChange change : versionChangeContext.getVersionChanges()) {
                if (change.getArtifactId().equals(feature.getId())
                        && change.getVersion().equals(feature.getVersion())) {
                    String error = Versions.validateOsgiVersion(change.getNewVersion(), getFeatureFile(project));
                    return error != null ? Collections.singleton(error) : null;
                }
            }
        }
        return null;
    }

    private void changeLicenseFeature(VersionChange change, Feature feature) {
        if (change.getArtifactId().equals(feature.getLicenseFeature())
                && change.getVersion().equals(feature.getLicenseFeatureVersion())) {
            logger.info("  feature.xml//feature/@license-feature='" + feature.getLicenseFeature()
                    + "'/@license-feature-version: " + change.getVersion() + " => " + change.getNewVersion());
            feature.setLicenseFeatureVersion(change.getNewVersion());
        }
    }

    private void changeIncludedFeatures(VersionChange change, Feature feature) {
        for (FeatureRef ref : feature.getIncludedFeatures()) {
            if (change.getArtifactId().equals(ref.getId()) && change.getVersion().equals(ref.getVersion())) {
                logger.info("  feature.xml//feature/includes/@id='" + ref.getId() + "'/@version: " + change.getVersion()
                        + " => " + change.getNewVersion());
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
            File file = getFeatureFile(project);
            try {
                feature = Feature.read(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read feature descriptor" + file, e);
            }
            project.putMetadata(feature);
        }
        return feature;
    }

    private File getFeatureFile(ProjectMetadata project) {
        return new File(project.getBasedir(), Feature.FEATURE_XML);
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        Feature feature = project.getMetadata(Feature.class);
        if (feature != null) {
            Feature.write(feature, getFeatureFile(project));
        }
    }
}
