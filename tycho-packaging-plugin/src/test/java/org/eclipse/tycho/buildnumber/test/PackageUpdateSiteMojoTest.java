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
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.packaging.PackageUpdateSiteMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class PackageUpdateSiteMojoTest extends AbstractTychoMojoTestCase {

    private MavenProject project;

    private PackageUpdateSiteMojo packagemojo;

    private File targetFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File basedir = getBasedir("projects/updateSitePackaging");
        File platform = new File("src/test/resources/eclipse");

        List<MavenProject> projects = getSortedProjects(basedir, platform);

        project = projects.get(0);
        targetFolder = new File(project.getFile().getParent(), "target");

        // simulate previous update site build.
        // performed by
        // org.eclipse.tycho:tycho-packaging-plugin:${project.version}:update-site,org.eclipse.tycho:tycho-p2-plugin:${project.version}:update-site-p2-metadata,
        File siteFolder = new File(targetFolder, "site");
        siteFolder.mkdirs();
        new File(siteFolder, "site.xml").createNewFile();
        new File(siteFolder, "content.xml").createNewFile();
        new File(siteFolder, "artifacts.xml").createNewFile();

        packagemojo = (PackageUpdateSiteMojo) lookupMojo("update-site-packaging", project.getFile());
        setVariableValueToObject(packagemojo, "project", project);
        setVariableValueToObject(packagemojo, "target", siteFolder);
    }

    public void testArchiveSite() throws Exception {
        setVariableValueToObject(packagemojo, "archiveSite", Boolean.TRUE);

        packagemojo.execute();
        checkSiteZip();

        File assemblyZip = new File(targetFolder, "site_assembly.zip");
        Assert.assertTrue(assemblyZip.exists());
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
        Assert.assertTrue(attachedArtifacts.size() == 1);
        Assert.assertTrue(attachedArtifacts.get(0).getFile().equals(assemblyZip));
        Assert.assertTrue(attachedArtifacts.get(0).getClassifier().equals("assembly"));
        Assert.assertTrue(attachedArtifacts.get(0).getType().equals("zip"));
        ZipFile zip = new ZipFile(assemblyZip);
        try {
            assertNotNull(zip.getEntry("site.xml"));
            assertNotNull(zip.getEntry("content.xml"));
        } finally {
            zip.close();
        }
    }

    public void testNoArchiveSite() throws Exception {
        // this is the default
        // setVariableValueToObject( packagemojo, "archiveSite", Boolean.FALSE );
        packagemojo.execute();
        checkSiteZip();
    }

    private void checkSiteZip() throws ZipException, IOException {
        File resultzip = new File(targetFolder, "site.zip");
        Assert.assertTrue(resultzip.exists());
        Assert.assertEquals(project.getArtifact().getFile(), resultzip);

        ZipFile zip = new ZipFile(resultzip);
        try {
            assertNotNull(zip.getEntry("site.xml"));
            assertNull(zip.getEntry("content.xml"));
        } finally {
            zip.close();
        }
    }

}
