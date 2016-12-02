/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds    
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.PomFile;

public abstract class ProductFileManipulator extends AbstractMetadataManipulator {

    protected void applyChangeToProduct(ProjectMetadata project, ProductConfiguration product, String productFileName,
            VersionChange change) {
        if (isSameProject(project, change.getProject())) {
            // in eclipse-repository, change.getArtifactId() doesn't have to match product.getId()
            if (change.getVersion().equals(product.getVersion())) {
                logger.info("  " + productFileName + "//product/@version: " + change.getVersion() + " => "
                        + change.getNewVersion());
                product.setVersion(change.getNewVersion());
            }
        } else if (isBundle(change.getProject())) {
            for (PluginRef plugin : product.getPlugins()) {
                if (change.getArtifactId().equals(plugin.getId()) && change.getVersion().equals(plugin.getVersion())) {
                    logger.info("  " + productFileName + "//product/plugins/plugin/@id=" + plugin.getId()
                            + "/@version: " + change.getVersion() + " => " + change.getNewVersion());
                    plugin.setVersion(change.getNewVersion());
                }
            }
        } else if (isFeature(change.getProject().getPackaging())) {
            for (FeatureRef feature : product.getFeatures()) {
                if (change.getArtifactId().equals(feature.getId()) && change.getVersion().equals(feature.getVersion())) {
                    logger.info("  " + productFileName + "//product/features/feature/@id=" + feature.getId()
                            + "/@version: " + change.getVersion() + " => " + change.getNewVersion());
                    feature.setVersion(change.getNewVersion());
                }
            }
        }
    }

    protected boolean isSameProject(ProjectMetadata project1, PomFile project2) {
        PomFile project1Pom = project1.getMetadata(PomFile.class);
        return project1Pom.getArtifactId().equals(project2.getArtifactId())
                && project1Pom.getGroupId().equals(project2.getGroupId());
    }

}
