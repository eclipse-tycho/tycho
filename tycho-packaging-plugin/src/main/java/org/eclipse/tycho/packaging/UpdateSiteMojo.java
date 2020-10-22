/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.UpdateSite;
import org.eclipse.tycho.model.UpdateSite.SiteFeatureRef;

@Mojo(name = "update-site", threadSafe = true)
public class UpdateSiteMojo extends AbstractTychoPackagingMojo {
    private static final Object LOCK = new Object();

    @Parameter(defaultValue = "${project.build.directory}/site")
    private File target;

    @Parameter(property = "project.basedir")
    private File basedir;

    @Parameter
    private boolean inlineArchives;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            target.mkdirs();
            try {
                // remove content collected in former builds.
                // Even without clean goal the build result must not assembly out dated content
                FileUtils.cleanDirectory(target);
            } catch (IOException e) {
                throw new MojoFailureException("Unable to delete old update site content: " + target.getAbsolutePath(),
                        e);
            }
            // expandVersion();

            try {
                UpdateSite site = UpdateSite.read(new File(basedir, UpdateSite.SITE_XML));

                UpdateSiteAssembler assembler = new UpdateSiteAssembler(plexus, target);
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
                        FileUtils.copyFile(srcAssociateSitesFile,
                                new File(target + File.separator + site.getAssociateSitesUrl()));
                    }
                }
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }
}
