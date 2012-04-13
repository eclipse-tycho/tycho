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
package org.eclipse.tycho.test.TYCHO491PublishFeaturesAndCategories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.TYCHO188P2EnabledRcp.Util;
import org.junit.Assert;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

public class Tycho491PublishFeaturesAndCategoriesTest extends AbstractTychoIntegrationTest {

    private static final String MODULE = "eclipse-repository";

    private static final String ARTIFACT_ID = "example-eclipse-repository";

    private static final String PROJECT_VERSION = "1.0.0-SNAPSHOT";

    private static final String QUALIFIER = "20101116-forcedQualifier";

    @Test
    public void testEclipseRepositoryWithIncludedFeatures() throws Exception {
        Verifier verifier = getVerifier("/TYCHO491PublishFeaturesAndCategories", false);

        /*
         * Do not execute "install" to ensure that features and bundles can be included directly
         * from the build results of the local reactor.
         */
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File repoDir = new File(verifier.getBasedir(), MODULE + "/target/repository");
        File contentJar = new File(repoDir, "content.jar");
        assertTrue("content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile());

        Document contentXml = Util.openXmlFromZip(contentJar, "content.xml");

        assertCategoryIU(contentXml, QUALIFIER + ".example.category", "example.feature.feature.group");

        assertFeatureIuAndArtifact(verifier, contentXml, "example.feature", "example.included.feature.feature.group",
                "example.bundle");
        assertBundleIuAndArtifact(verifier, contentXml, "example.bundle");
        assertBundleIuAndArtifact(verifier, contentXml, "org.eclipse.core.contenttype", "3.4.100.v20100505-1235"); // a bundle from the target platform

        assertFeatureIuAndArtifact(verifier, contentXml, "example.included.feature", "example.included.bundle");
        assertBundleIuAndArtifact(verifier, contentXml, "example.included.bundle");

    }

    static private void assertCategoryIU(Document contentXml, String categoryIuId, String featureIuId) {
        Set<Element> categoryIus = Util.findIU(contentXml, categoryIuId);
        assertEquals("Unique category iu not found", 1, categoryIus.size());
        Element categoryIu = categoryIus.iterator().next();

        assertTrue("iu not typed as category",
                Util.iuHasProperty(categoryIu, "org.eclipse.equinox.p2.type.category", "true"));
        assertTrue("category name missing",
                Util.iuHasProperty(categoryIu, "org.eclipse.equinox.p2.name", "Example Category"));
        assertTrue(Util.iuHasAllRequirements(categoryIu, featureIuId));
    }

    static private void assertFeatureIuAndArtifact(Verifier verifier, Document contentXml, String featureId,
            String... requiredIus) throws IOException {
        String featureIuId = featureId + ".feature.group";
        Set<Element> featureIus = Util.findIU(contentXml, featureIuId);
        assertEquals("Unique feature iu not found", 1, featureIus.size());
        Element featureIu = featureIus.iterator().next();

        assertTrue(Util.containsIUWithProperty(contentXml, featureIuId, "org.eclipse.equinox.p2.type.group", "true"));
        assertTrue(Util.iuHasAllRequirements(featureIu, requiredIus));

        String featureArtifactPath = "features/" + featureId + "_1.0.0." + QUALIFIER + ".jar";
        assertRepositoryContainsArtifact(verifier, featureArtifactPath);
    }

    static private void assertBundleIuAndArtifact(Verifier verifier, Document contentXml, String bundleId)
            throws IOException {
        assertBundleIuAndArtifact(verifier, contentXml, bundleId, "1.0.0." + QUALIFIER);
    }

    static private void assertBundleIuAndArtifact(Verifier verifier, Document contentXml, String bundleId,
            String version) throws IOException

    {
        assertTrue("bundle not found", Util.containsIU(contentXml, bundleId));

        String bundleArtifactPath = "plugins/" + bundleId + '_' + version + ".jar";
        assertRepositoryContainsArtifact(verifier, bundleArtifactPath);
    }

    static private void assertRepositoryContainsArtifact(Verifier verifier, String artifactPath) throws IOException,
            ZipException {
        File repositoryArtifact = new File(verifier.getBasedir(), MODULE + "/target/" + ARTIFACT_ID + "-"
                + PROJECT_VERSION + ".zip");
        assertZipContainsEntry(repositoryArtifact, artifactPath);
    }

    private static void assertZipContainsEntry(File file, String path) throws IOException {
        ZipFile zipFile = new ZipFile(file);

        for (final Enumeration<?> entries = zipFile.getEntries(); entries.hasMoreElements();) {
            final ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().equals(path)) {
                return;
            }
        }
        Assert.fail("missing entry " + path + " in repository archive " + file);
    }
}
