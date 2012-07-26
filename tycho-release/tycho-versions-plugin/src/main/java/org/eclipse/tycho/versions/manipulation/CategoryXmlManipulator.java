/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.MutablePomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-repository")
public class CategoryXmlManipulator extends AbstractMetadataManipulator {

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (isCategory(project)) {
            if (isFeature(change.getProject().getPackaging())) {
                Category site = getCategory(project);

                for (FeatureRef feature : site.getFeatures()) {
                    if (change.getArtifactId().equals(feature.getId())
                            && change.getVersion().equals(feature.getVersion())) {
                        logger.info("  category.xml//site/feature/@id=" + feature.getId() + "/@version: "
                                + change.getVersion() + " => " + change.getNewVersion());
                        feature.setVersion(change.getNewVersion());
                    }
                }
            }
        }
    }

    String rewriteFeatureUrl(String url, VersionChange change) {
        if (url != null) {
            return url.replaceAll("\\Q" + change.getVersion() + "\\E", change.getNewVersion());
        }
        return null;
    }

    private Category getCategory(ProjectMetadata project) {
        Category site = project.getMetadata(Category.class);
        if (site == null) {
            File file = new File(project.getBasedir(), Category.CATEGORY_XML);
            try {
                site = Category.read(file);
                project.putMetadata(site);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read update site " + file, e);
            }
        }

        return site;
    }

    private boolean isCategory(ProjectMetadata project) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        return isCategory(pom.getPackaging());
    }

    private boolean isCategory(String packaging) {
        return ArtifactKey.TYPE_ECLIPSE_REPOSITORY.equals(packaging);
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        File basedir = project.getBasedir();
        Category site = project.getMetadata(Category.class);
        if (site != null) {
            Category.write(site, new File(basedir, Category.CATEGORY_XML));
        }
    }
}
