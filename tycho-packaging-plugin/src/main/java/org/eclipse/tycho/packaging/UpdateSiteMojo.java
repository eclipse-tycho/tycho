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
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.UpdateSite;
import org.eclipse.tycho.model.UpdateSite.SiteFeatureRef;

/**
 * @goal update-site
 */
public class UpdateSiteMojo extends AbstractTychoPackagingMojo {

    /** @parameter expression="${project.build.directory}/site" */
    private File target;

    /** @parameter expression="${project.basedir}" */
    private File basedir;

    /** @parameter */
    private boolean inlineArchives;

    public void execute() throws MojoExecutionException, MojoFailureException {
        target.mkdirs();
        try {
            // remove content collected in former builds.
            // Even without clean goal the build result must not assembly out dated content
            FileUtils.cleanDirectory(target);
        } catch (IOException e) {
            throw new MojoFailureException("Unable to delete old update site content: " + target.getAbsolutePath(), e);
        }
        // expandVersion();

        try {
            UpdateSite site = UpdateSite.read(new File(basedir, UpdateSite.SITE_XML));

            UpdateSiteAssembler assembler = new UpdateSiteAssembler(session, target);
            assembler.setPack200(site.isPack200());
            if (inlineArchives) {
                assembler.setArchives(site.getArchives());
            }

            getDependencyWalker().walk(assembler);
            getDependencyWalker().traverseUpdateSite(site, new ArtifactDependencyVisitor() {
                @Override
                public boolean visitFeature(FeatureDescription feature) {
                    FeatureRef featureRef = feature.getFeatureRef();
                    String id = featureRef.getId();
                    ReactorProject otherProject = feature.getMavenProject();
                    String version;
                    if (otherProject != null) {
                        version = otherProject.getExpandedVersion();
                    } else {
                        version = feature.getKey().getVersion();
                    }
                    String url = UpdateSiteAssembler.FEATURES_DIR + id + "_" + version + ".jar";
                    ((SiteFeatureRef) featureRef).setUrl(url);
                    featureRef.setVersion(version);
                    return false; // don't traverse included features
                }
            });

            if (inlineArchives) {
                site.removeArchives();
            }

            File file = new File(target, "site.xml");
            UpdateSite.write(site, file);
			
			// Copy the associate sites file, if necessary
			if (site.getAssociateSitesUrl() != null) {
				File srcAssociateSitesFile = new File(basedir, site.getAssociateSitesUrl());
				if (srcAssociateSitesFile.exists()) {
					FileUtils.copyFile(srcAssociateSitesFile, new File(target + File.separator + site.getAssociateSitesUrl()));
				}
			}
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
