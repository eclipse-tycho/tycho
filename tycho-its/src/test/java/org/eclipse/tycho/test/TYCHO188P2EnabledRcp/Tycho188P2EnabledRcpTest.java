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
package org.eclipse.tycho.test.TYCHO188P2EnabledRcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.pdark.decentxml.Document;

public class Tycho188P2EnabledRcpTest extends AbstractTychoIntegrationTest {

    private static final String MODULE = "eclipse-repository";

    private static final String GROUP_ID = "org.eclipse.tycho.tychoits.TYCHO188";

    private static final String ARTIFACT_ID = "example-eclipse-repository";

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static final List<Product> TEST_PRODUCTS = Arrays.asList(new Product("main.product.id", "", false, true),
            new Product("extra.product.id", "extra", "rootfolder", true, false), new Product("repoonly.product.id",
                    false));

    private static final List<Environment> TEST_ENVIRONMENTS = Arrays.asList(new Environment("win32", "win32", "x86"),
            new Environment("linux", "gtk", "x86"));

    private static Verifier verifier;

    @BeforeClass
    public static void buildProduct() throws Exception {
        verifier = new Tycho188P2EnabledRcpTest().getVerifier("/TYCHO188P2EnabledRcp", false);

        verifier.getCliOptions().add("-Pbuild-products");
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testInstalledProdcutArtifacts() throws Exception {
        for (Product product : TEST_PRODUCTS) {
            for (Environment env : TEST_ENVIRONMENTS) {
                assertProductArtifacts(verifier, product, env);
            }
        }
    }

    @Test
    public void testPublishedProducts() throws Exception {
        validatePublishedProducts(verifier, getContentXml(verifier));
    }

    @Test
    public void testContent() throws Exception {
        assertRepositoryArtifacts(verifier);
        int environmentsPerProduct = TEST_ENVIRONMENTS.size();

        int publishedArtifacts = TEST_PRODUCTS.size() * environmentsPerProduct; // the branded executables, produced by the ProductAction

        int materializedProducts = TEST_PRODUCTS.size() - 1;
        int distributionArtifacts = materializedProducts * environmentsPerProduct;

        int repositoryArtifacts = 1;
        assertTotalZipArtifacts(verifier, publishedArtifacts + distributionArtifacts + repositoryArtifacts);
    }

    private static void validatePublishedProducts(Verifier verifier, Document contentXml) throws IOException,
            ZipException {
        for (Product product : TEST_PRODUCTS) {
            for (Environment env : TEST_ENVIRONMENTS) {
                assertProductIUs(contentXml, product, env);
            }
        }
    }

    private static Document getContentXml(Verifier verifier) throws IOException, ZipException {
        File repoDir = new File(verifier.getBasedir(), MODULE + "/target/repository");
        File contentJar = new File(repoDir, "content.jar");
        assertTrue("content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile());
        Document contentXml = Util.openXmlFromZip(contentJar, "content.xml");
        return contentXml;
    }

    static private void assertProductIUs(Document contentXml, Product product, Environment env) {
        assertTrue(product.unitId + " IU with lineUp property value true does not exist",
                Util.containsIUWithProperty(contentXml, product.unitId, "lineUp", "true"));

        final String p2InfAdded = "p2.inf.added-property";
        assertEquals("Property " + p2InfAdded + " in " + product.unitId, product.p2InfProperty,
                Util.containsIUWithProperty(contentXml, product.unitId, p2InfAdded, "true"));

        /*
         * This only works if the context repositories are configured correctly. If the
         * simpleconfigurator bundle is not visible to the product publisher, this IU would not be
         * generated.
         */
        String simpleConfiguratorIU = "tooling" + env.toWsOsArch() + "org.eclipse.equinox.simpleconfigurator";
        assertTrue(simpleConfiguratorIU + " IU does not exist", Util.containsIU(contentXml, simpleConfiguratorIU));
    }

    static private void assertProductArtifacts(Verifier verifier, Product product, Environment env) throws IOException,
            ZipException {
        if (product.isMaterialized()) {
            File artifactDirectory = new File(verifier.getArtifactPath(GROUP_ID, ARTIFACT_ID, VERSION, "zip"))
                    .getParentFile();
            File installedProductArchive = new File(artifactDirectory, ARTIFACT_ID + '-' + VERSION
                    + product.getAttachIdSegment() + "-" + env.toOsWsArch() + ".zip");
            assertTrue("Product archive not found at: " + installedProductArchive, installedProductArchive.exists());

            String rootFolder = product.getRootFolderName() != null ? product.getRootFolderName() + "/" : "";

            Properties configIni = Util.openPropertiesFromZip(installedProductArchive, rootFolder
                    + "configuration/config.ini");
            String bundleConfiguration = configIni.getProperty("osgi.bundles");
            assertTrue("Installation is not configured to use the simpleconfigurator",
                    bundleConfiguration.startsWith("reference:file:org.eclipse.equinox.simpleconfigurator"));
            // TODO all these assertions should be in the test method directly
            String expectedProfileName = env.os.equals("linux") ? "ProfileNameForLinux"
                    : "ConfiguredDefaultProfileName";
            assertEquals("eclipse.p2.profile in config.ini", expectedProfileName,
                    configIni.getProperty("eclipse.p2.profile"));

            assertRootFolder(installedProductArchive, product.getRootFolderName());

            if (product.hasLocalFeature()) {
                assertContainsEntry(installedProductArchive, rootFolder + "features/example.feature_1.0.0.");
                assertContainsEntry(installedProductArchive, rootFolder + "plugins/example.bundle_1.0.0.");
            }
        }
    }

    private static void assertContainsEntry(File file, String prefix) throws IOException {
        ZipFile zipFile = new ZipFile(file);

        try {
            for (final Enumeration<?> entries = zipFile.entries(); entries.hasMoreElements();) {
                final ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().startsWith(prefix)) {
                    if (entry.getName().endsWith("qualifier")) {
                        Assert.fail("replacement of build qualifier missing in " + file + ", zip entry: "
                                + entry.getName());
                    }
                    return;
                }
            }

            Assert.fail("missing entry " + prefix + "* in product archive " + file);
        } finally {
            zipFile.close();
        }
    }

    private static void assertRootFolder(File file, String rootFolderName) throws IOException {
        if (rootFolderName != null) {
            ZipFile zipFile = new ZipFile(file);
            try {
                rootFolderName += "/";
                Assert.assertNotNull(file.getName() + " does not contain the rootfolder \"" + rootFolderName + "\"",
                        zipFile.getEntry(rootFolderName));
                String entry = rootFolderName + "configuration/config.ini";
                Assert.assertNotNull(file.getName() + " does not contain path in the rootfolder \"" + entry + "\"",
                        zipFile.getEntry(entry));
            } finally {
                zipFile.close();
            }
        }
    }

    static private void assertRepositoryArtifacts(Verifier verifier) {
        verifier.assertArtifactPresent(GROUP_ID, ARTIFACT_ID, VERSION, "zip");
    }

    static private void assertTotalZipArtifacts(final Verifier verifier, final int expectedArtifacts) {
        final File artifactDirectory = new File(verifier.getArtifactPath(GROUP_ID, ARTIFACT_ID, VERSION, "zip"))
                .getParentFile();
        final String prefix = ARTIFACT_ID + '-' + VERSION;

        int zipArtifacts = 0;
        for (final String fileName : artifactDirectory.list()) {
            if (fileName.startsWith(prefix) && fileName.endsWith(".zip")) {
                zipArtifacts++;
            }
        }
        assertEquals(expectedArtifacts, zipArtifacts);
    }

    static class Environment {
        String os;

        String ws;

        String arch;

        Environment(String os, String ws, String arch) {
            this.os = os;
            this.ws = ws;
            this.arch = arch;
        }

        String toOsWsArch() {
            return os + '.' + ws + '.' + arch;
        }

        String toWsOsArch() {
            return ws + '.' + os + '.' + arch;
        }
    }

    static class Product {
        String unitId;

        String attachId;

        boolean p2InfProperty;

        private final boolean localFeature;

        private final String rootFolderName;

        Product(String unitId, String attachId, String rootFolderName, boolean p2InfProperty, boolean localFeature) {
            this.unitId = unitId;
            this.attachId = attachId;
            this.p2InfProperty = p2InfProperty;
            this.localFeature = localFeature;
            this.rootFolderName = rootFolderName;
        }

        Product(String unitId, String attachId, boolean p2InfProperty, boolean localFeature) {
            this(unitId, attachId, null, p2InfProperty, localFeature);
        }

        Product(String unitId, boolean p2InfProperty) {
            this(unitId, null, null, p2InfProperty, false);
        }

        boolean isMaterialized() {
            return attachId != null;
        }

        String getAttachIdSegment() {
            if (attachId == null) {
                throw new IllegalStateException();
            }
            return attachId.length() == 0 ? "" : "-" + attachId;
        }

        boolean hasLocalFeature() {
            return localFeature;
        }

        public String getRootFolderName() {
            return rootFolderName;
        }
    }
}
