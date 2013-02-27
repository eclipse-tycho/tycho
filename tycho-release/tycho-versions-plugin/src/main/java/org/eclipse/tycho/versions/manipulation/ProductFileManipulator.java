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

import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.MutablePomFile;

public abstract class ProductFileManipulator extends AbstractMetadataManipulator {

    protected void applyChangeToProduct(ProjectMetadata project, ProductConfiguration product, String productFileName,
            VersionChange change) {
        if (isSameProject(project, change.getProject())) {
            // in eclipse-repository, change.getArtifactId() doesn't have to match product.getId()
            // and change.getVersion() doesn't have to match product.getVersion()
            logger.info("  " + productFileName + "//product/@version: " + change.getVersion() + " => "
                    + change.getNewVersion());
            product.setVersion(change.getNewVersion());
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

    private boolean isSameProject(ProjectMetadata project1, MutablePomFile project2) {
        MutablePomFile project1Pom = project1.getMetadata(MutablePomFile.class);
        return project1Pom.getArtifactId().equals(project2.getArtifactId())
                && project1Pom.getEffectiveGroupId().equals(project2.getEffectiveGroupId());
    }

}
