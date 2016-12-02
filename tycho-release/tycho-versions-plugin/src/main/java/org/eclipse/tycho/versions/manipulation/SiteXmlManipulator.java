/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Beat Strasser (Inventage AG) - preserve feature url in site.xml
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.UpdateSite;
import org.eclipse.tycho.model.UpdateSite.SiteFeatureRef;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.pom.PomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-update-site")
public class SiteXmlManipulator extends AbstractMetadataManipulator {

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isSite(project)) {
            for (VersionChange change : versionChangeContext.getVersionChanges()) {
                if (isFeature(change.getProject().getPackaging())) {
                    UpdateSite site = getSiteXml(project);

                    for (FeatureRef feature : site.getFeatures()) {
                        if (change.getArtifactId().equals(feature.getId())
                                && change.getVersion().equals(feature.getVersion())) {
                            logger.info("  site.xml//site/feature/@id=" + feature.getId() + "/@version: "
                                    + change.getVersion() + " => " + change.getNewVersion());
                            feature.setVersion(change.getNewVersion());

                            SiteFeatureRef siteFeature = (SiteFeatureRef) feature;
                            String oldUrl = siteFeature.getUrl();
                            String newUrl = rewriteFeatureUrl(oldUrl, change);
                            logger.info("  site.xml//site/feature/@id=" + feature.getId() + "/@url: " + oldUrl + " => "
                                    + newUrl);
                            siteFeature.setUrl(newUrl);
                        }
                    }
                }
            }
        }
    }

    static String rewriteFeatureUrl(String url, VersionChange change) {
        if (url != null) {
            return url.replaceAll("\\Q" + change.getVersion() + "\\E", change.getNewVersion());
        }
        return null;
    }

    private UpdateSite getSiteXml(ProjectMetadata project) {
        UpdateSite site = project.getMetadata(UpdateSite.class);
        if (site == null) {
            File file = new File(project.getBasedir(), UpdateSite.SITE_XML);
            try {
                site = UpdateSite.read(file);
                project.putMetadata(site);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read update site " + file, e);
            }
        }

        return site;
    }

    private boolean isSite(ProjectMetadata project) {
        PomFile pom = project.getMetadata(PomFile.class);
        return isSite(pom.getPackaging());
    }

    private boolean isSite(String packaging) {
        return PackagingType.TYPE_ECLIPSE_UPDATE_SITE.equals(packaging);
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        File basedir = project.getBasedir();
        UpdateSite site = project.getMetadata(UpdateSite.class);
        if (site != null) {
            UpdateSite.write(site, new File(basedir, UpdateSite.SITE_XML));
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        return null; // there is no project version in site.xml
    }
}
