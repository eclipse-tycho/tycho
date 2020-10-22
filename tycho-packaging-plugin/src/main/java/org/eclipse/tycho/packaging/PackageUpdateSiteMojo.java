/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

@Mojo(name = "update-site-packaging", threadSafe = true)
public class PackageUpdateSiteMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Generated update site location (must match update-site mojo configuration)
     */
    @Parameter(defaultValue = "${project.build.directory}/site")
    private File target;

    /**
     * If true, create site assembly zip file. If false (the default), do not create site assembly
     * zip file.
     * 
     * Please note that the project's main artifact that will be deployed/installed to maven
     * repository is a zip only containing the site.xml. However, if this parameter is set to true
     * an additional result file classified as 'assembly' containing a full packaged update site
     * will be created and installed.
     */
    @Parameter(defaultValue = "false")
    private boolean archiveSite;

    /**
     * Used for attaching assembled update site to the project.
     */
    @Component
    private MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (target == null || !target.isDirectory()) {
            throw new MojoExecutionException(
                    "Update site folder does not exist at: " + target != null ? target.getAbsolutePath() : "null");
        }

        synchronized (LOCK) {
            try {
                ZipArchiver siteZipper = new ZipArchiver();
                File siteDestination = new File(target.getParentFile(), "site.zip");
                siteZipper.addFile(new File(target, "site.xml"), "site.xml");
                siteZipper.setDestFile(siteDestination);
                siteZipper.createArchive();
                project.getArtifact().setFile(siteDestination);
                if (archiveSite) {
                    ZipArchiver asssemblyZipper = new ZipArchiver();
                    File asssemblyDestFile = new File(target.getParentFile(), "site_assembly.zip");
                    asssemblyZipper.addDirectory(target);
                    asssemblyZipper.setDestFile(asssemblyDestFile);
                    asssemblyZipper.createArchive();
                    projectHelper.attachArtifact(project, "zip", "assembly", asssemblyDestFile);
                }

            } catch (IOException | ArchiverException e) {
                throw new MojoExecutionException("Error packing update site", e);
            }
        }
    }

}
