/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @goal update-site-packaging
 */
public class PackageUpdateSiteMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * Generated update site location (must match update-site mojo configuration)
     * 
     * @parameter expression="${project.build.directory}/site"
     */
    private File target;

    /**
     * If true, create site assembly zip file. If false (the default), do not create site assembly
     * zip file.
     * 
     * Please note that the project's main artifact that will be deployed/installed to maven
     * repository is a zip only containing the site.xml. However, if this parameter is set to true
     * an additional result file classified as 'assembly' containing a full packaged update site
     * will be created and installed.
     * 
     * @parameter default-value="false"
     */
    private boolean archiveSite;

    /**
     * Used for attaching assembled update site to the project.
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (target == null || !target.isDirectory()) {
            throw new MojoExecutionException(
                    "Update site folder does not exist at: " + target != null ? target.getAbsolutePath() : "null");
        }

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

        } catch (IOException e) {
            throw new MojoExecutionException("Error packing update site", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error packing update site", e);
        }
    }

}
