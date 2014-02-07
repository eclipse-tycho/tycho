/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PublishProductMojoUnitTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private final File testResources = new File("src/test/resources/unitTestResources");

    @Test
    public void testQualifyVersions() throws IOException {
        ProductConfiguration product = ProductConfiguration.read(new File(testResources, "test.product"));

        PublishProductMojo.qualifyVersions(product, "20100623");

        assertEquals("0.1.0.20100623", product.getVersion());
        assertEquals("0.1.0.20100623", product.getFeatures().get(0).getVersion());
        assertEquals("0.1.0.qual", product.getFeatures().get(1).getVersion());
        assertEquals("0.1.0.20100623", product.getPlugins().get(0).getVersion());
        assertEquals("0.1.0.qual", product.getPlugins().get(1).getVersion());
    }

    @Test
    public void testQualifyVersionsNoVersions() throws IOException {
        ProductConfiguration product = ProductConfiguration.read(new File(testResources, "noVersion.product"));

        PublishProductMojo.qualifyVersions(product, "20100623");

        assertNull(product.getVersion());
    }

    @Test
    public void testQualifyVersionsEmptyVersions() throws IOException {
        ProductConfiguration product = ProductConfiguration.read(new File(testResources, "emptyVersion.product"));

        PublishProductMojo.qualifyVersions(product, "20100623");

        assertEquals("", product.getVersion());
    }

    // TODO test through the right interface: we should do assertions on the resulting IUs rather making assumptions on the obscure behaviour of p2's ProductAction
    @Test
    public void testCopyFilesAndWriteQualifiedVersions() throws Exception {
        File productFile = new File(testResources, "test.product");
        ProductConfiguration productConfiguration = ProductConfiguration.read(productFile);
        BuildOutputDirectory buildBasedir = new BuildOutputDirectory(tempFolder.newFolder("buildBasedir"));

        PublishProductMojo.qualifyVersions(productConfiguration, "buildQualifier");
        File preparedProduct = PublishProductMojo.writeProductForPublishing(productFile, productConfiguration,
                buildBasedir);

        assertTrue(buildBasedir.getChild("products/testproduct/p2.inf").exists());

        File buildProductRootDir = buildBasedir.getChild("products/testproduct");
        assertFileExists("icons/linux.xpm", buildProductRootDir);
        assertFileExists("icons/mac.ico", buildProductRootDir);
        assertFileExists("icons/solaris.ico", buildProductRootDir);
        assertFileExists("icons/win.ico", buildProductRootDir);
        assertFileExists("configs/config_linux.ini", buildProductRootDir);
        assertFileExists("configs/config_macosx.ini", buildProductRootDir);
        assertFileExists("configs/config_win32.ini", buildProductRootDir);
        assertFileExists("configs/config_solaris.ini", buildProductRootDir);
        ProductConfiguration buildProductConfiguration = ProductConfiguration.read(preparedProduct);
        assertEquals("0.1.0.buildQualifier", buildProductConfiguration.getVersion());
    }

    private void assertFileExists(String relativePath, File dir) {
        assertTrue(new File(dir, relativePath).isFile());
    }

    @Test
    public void testQualifyVersionsWithEmptyQualifier() throws Exception {
        File productFile = new File(testResources, "test.product");
        ProductConfiguration productConfiguration = ProductConfiguration.read(productFile);
        BuildOutputDirectory buildBasedir = new BuildOutputDirectory(tempFolder.newFolder("buildBasedir"));

        PublishProductMojo.qualifyVersions(productConfiguration, "");
        File preparedProduct = PublishProductMojo.writeProductForPublishing(productFile, productConfiguration,
                buildBasedir);

        ProductConfiguration buildProductConfiguration = ProductConfiguration.read(preparedProduct);
        assertEquals("0.1.0", buildProductConfiguration.getVersion());
    }

    @Test
    public void testCopyMissingP2Inf() throws IOException {
        File sourceDirectory = tempFolder.newFolder("source");
        File targetDirectory = tempFolder.newFolder("target");

        File productFile = new File(sourceDirectory, "test.product");
        productFile.createNewFile();

        File p2InfTarget = new File(targetDirectory, "p2.inf");
        PublishProductMojo.copyP2Inf(PublishProductMojo.getSourceP2InfFile(productFile), p2InfTarget);

        assertFalse(p2InfTarget.exists());
    }

    @Test
    public void testGetSourceP2InfFile() throws IOException {
        String p2InfFile = PublishProductMojo.getSourceP2InfFile(new File("./test/test.product")).getCanonicalPath();
        assertEquals(new File("./test/test.p2.inf").getCanonicalPath(), p2InfFile);
    }

    @Test
    public void testExtractRootFeatures() throws Exception {
        ProductConfiguration product = ProductConfiguration.read(new File(testResources, "rootFeatures.product"));
        List<DependencySeed> result = new ArrayList<DependencySeed>();

        PublishProductMojo.extractRootFeatures(product, result);

        // workaround for 428889: remove from root features so that p2 doesn't include them in the product IU
        List<String> featuresInProduct = getIDs(product.getFeatures());
        assertThat(featuresInProduct, hasItem("org.eclipse.rcp"));
        assertThat(featuresInProduct, not(hasItem("org.eclipse.help")));
        assertThat(featuresInProduct, not(hasItem("org.eclipse.egit")));

        assertThat(result.size(), not(is(0)));
        assertThat(result.get(0).getId(), is("org.eclipse.help"));
        assertThat(result.get(1).getId(), is("org.eclipse.egit"));
        assertThat(result.size(), is(2));
    }

    private static List<String> getIDs(List<FeatureRef> features) {
        List<String> result = new ArrayList<String>();
        for (FeatureRef feature : features) {
            result.add(feature.getId());
        }
        return result;
    }

}
