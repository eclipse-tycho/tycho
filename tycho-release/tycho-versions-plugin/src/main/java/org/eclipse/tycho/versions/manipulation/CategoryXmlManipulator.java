/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import static org.eclipse.tycho.versions.manipulation.SiteXmlManipulator.rewriteFeatureUrl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.UpdateSite.SiteFeatureRef;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.pom.PomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-repository")
public class CategoryXmlManipulator extends AbstractMetadataManipulator {

    private static final String SOURCE_FEATURE_SUFFIX = ".source";

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseRepository(project)) {
            for (VersionChange versionChange : versionChangeContext.getVersionChanges()) {
                if (isFeature(versionChange.getProject().getPackaging())) {
                    updateFeatureReferences(versionChange, project);
                } else if (isBundle(versionChange.getProject())) {
                    updatePluginReferences(versionChange, project);
                }
            }
        }
    }

    protected void updateFeatureReferences(VersionChange featureVersionChange, ProjectMetadata project) {
        Category categoryXml = getCategoryXml(project);
        if (categoryXml == null) {
            return;
        }
        for (SiteFeatureRef feature : categoryXml.getFeatures()) {
            String featureId = featureVersionChange.getArtifactId();
            String srcFeatureId = featureId + SOURCE_FEATURE_SUFFIX;
            if ((featureId.equals(feature.getId()) || srcFeatureId.equals(feature.getId()))
                    && featureVersionChange.getVersion().equals(feature.getVersion())) {
                logger.info("  category.xml//site/feature[@id=" + feature.getId() + "]/@version: "
                        + featureVersionChange.getVersion() + " => " + featureVersionChange.getNewVersion());
                feature.setVersion(featureVersionChange.getNewVersion());

                String oldUrl = feature.getUrl();
                if (oldUrl != null) {
                    String newUrl = rewriteFeatureUrl(oldUrl, featureVersionChange);
                    logger.info("  category.xml//site/feature[@id=" + feature.getId() + "]/@url: " + oldUrl + " => "
                            + newUrl);
                    feature.setUrl(newUrl);
                }
            }
        }
    }

    private void updatePluginReferences(VersionChange pluginVersionChange, ProjectMetadata project) {
        Category categoryXml = getCategoryXml(project);
        if (categoryXml == null) {
            return;
        }
        for (PluginRef plugin : categoryXml.getPlugins()) {
            if (pluginVersionChange.getArtifactId().equals(plugin.getId())
                    && pluginVersionChange.getVersion().equals(plugin.getVersion())) {
                logger.info("  category.xml//site/bundle[@id=" + plugin.getId() + "]/@version: "
                        + pluginVersionChange.getVersion() + " => " + pluginVersionChange.getNewVersion());
                plugin.setVersion(pluginVersionChange.getNewVersion());
            }
        }
    }

    private Category getCategoryXml(ProjectMetadata project) {
        Category categoryXml = project.getMetadata(Category.class);
        if (categoryXml == null) {
            File file = new File(project.getBasedir(), Category.CATEGORY_XML);
            if (!file.isFile()) {
                return null;
            }
            try {
                categoryXml = Category.read(file);
                project.putMetadata(categoryXml);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read categories from " + file, e);
            }
        }

        return categoryXml;
    }

    private boolean isEclipseRepository(ProjectMetadata project) {
        PomFile pom = project.getMetadata(PomFile.class);
        return PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(pom.getPackaging());
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        File basedir = project.getBasedir();
        Category categoryXml = project.getMetadata(Category.class);
        if (categoryXml != null) {
            Category.write(categoryXml, new File(basedir, Category.CATEGORY_XML));
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        // this manipulator does not add any restrictions on version changes allowed for eclipse-repository projects
        return null;
    }
}
