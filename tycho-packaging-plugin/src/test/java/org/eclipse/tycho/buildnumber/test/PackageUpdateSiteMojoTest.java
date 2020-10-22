/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
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
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.eclipse.tycho.packaging.PackageUpdateSiteMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;

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
        File siteXml = new File(siteFolder, "site.xml");
        siteXml.createNewFile();
        FileUtils.fileAppend(siteXml.getAbsolutePath(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><site associateSitesURL=\"associate-sites.xml\"></site>");
        File associateSitesFile = new File(siteFolder, "associate-sites.xml");
        FileUtils.fileAppend(associateSitesFile.getAbsolutePath(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><associateSites><associateSite url=\"https://download.eclipse.org/technology/m2e/updates/M\" label=\"m2e site\" /></associateSites>");
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
        try (ZipFile zip = new ZipFile(assemblyZip)) {
            assertNotNull(zip.getEntry("site.xml"));
            assertNotNull(zip.getEntry("content.xml"));
        }
    }

    public void testNoArchiveSite() throws Exception {
        // this is the default
        // setVariableValueToObject( packagemojo, "archiveSite", Boolean.FALSE );
        packagemojo.execute();
        checkSiteZip();
    }

    public void testAssociateSitsURLSite() throws Exception {
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
        try (ZipFile zip = new ZipFile(assemblyZip)) {
            assertNotNull(zip.getEntry("associate-sites.xml"));
        }
    }

    private void checkSiteZip() throws ZipException, IOException {
        File resultzip = new File(targetFolder, "site.zip");
        Assert.assertTrue(resultzip.exists());
        Assert.assertEquals(project.getArtifact().getFile(), resultzip);

        try (ZipFile zip = new ZipFile(resultzip)) {
            assertNotNull(zip.getEntry("site.xml"));
            assertNull(zip.getEntry("content.xml"));
        }
    }

}
