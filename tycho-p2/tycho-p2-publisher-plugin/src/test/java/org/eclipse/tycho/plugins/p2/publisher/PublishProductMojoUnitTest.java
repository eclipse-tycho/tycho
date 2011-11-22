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
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.Product;
import org.eclipse.tycho.testing.TestUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PublishProductMojoUnitTest {
    private File tempDir;

    private File sourceDirectory;

    private File targetDirectory;

    @Before
    public void setUp() throws IOException {
        tempDir = createTempDir(getClass().getSimpleName());
        sourceDirectory = new File(tempDir, "source");
        sourceDirectory.mkdirs();
        targetDirectory = new File(tempDir, "target");
        targetDirectory.mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testQualifyVersions() throws IOException {
        File basedir = TestUtil.getBasedir("unitTestResources");
        File productFile = new File(basedir, "test.product");
        ProductConfiguration product = ProductConfiguration.read(productFile);
        PublishProductMojo.qualifyVersions(product, "20100623");

        Assert.assertEquals("0.1.0.20100623", product.getVersion());
        Assert.assertEquals("0.1.0.20100623", product.getFeatures().get(0).getVersion());
        Assert.assertEquals("0.1.0.qual", product.getFeatures().get(1).getVersion());
        Assert.assertEquals("0.1.0.20100623", product.getPlugins().get(0).getVersion());
        Assert.assertEquals("0.1.0.qual", product.getPlugins().get(1).getVersion());
    }

    @Test
    public void testQualifyVersionsNoVersions() throws IOException {
        File basedir = TestUtil.getBasedir("unitTestResources");
        File productFile = new File(basedir, "noVersion.product");
        ProductConfiguration product = ProductConfiguration.read(productFile);
        PublishProductMojo.qualifyVersions(product, "20100623");

        Assert.assertNull(product.getVersion());
    }

    @Test
    public void testQualifyVersionsEmptyVersions() throws IOException {
        File basedir = TestUtil.getBasedir("unitTestResources");
        File productFile = new File(basedir, "emptyVersion.product");
        ProductConfiguration product = ProductConfiguration.read(productFile);
        PublishProductMojo.qualifyVersions(product, "20100623");

        Assert.assertEquals("", product.getVersion());
    }

    @Test
    public void testPrepareBuildProduct() throws Exception {
        File basedir = TestUtil.getBasedir("unitTestResources");
        File productFile = new File(basedir, "test.product");
        Product product = new Product(productFile);
        BuildOutputDirectory buildBasedir = new BuildOutputDirectory(new File(tempDir, "buildBasedir"));
        Product buildProduct = PublishProductMojo.prepareBuildProduct(product, buildBasedir, "buildQualifier");

        Assert.assertEquals(buildBasedir.getChild("products/testproduct/p2.inf"), buildProduct.getP2infFile());
        Assert.assertTrue(buildBasedir.getChild("products/testproduct/p2.inf").exists());

        File buildProductRootDir = buildBasedir.getChild("products/testproduct");
        assertFileExists("icons/linux.xpm", buildProductRootDir);
        assertFileExists("icons/mac.ico", buildProductRootDir);
        assertFileExists("icons/solaris.ico", buildProductRootDir);
        assertFileExists("icons/win.ico", buildProductRootDir);
        assertFileExists("configs/config_linux.ini", buildProductRootDir);
        assertFileExists("configs/config_macosx.ini", buildProductRootDir);
        assertFileExists("configs/config_win32.ini", buildProductRootDir);
        assertFileExists("configs/config_solaris.ini", buildProductRootDir);
        ProductConfiguration productConfiguration = ProductConfiguration.read(buildProduct.getProductFile());
        Assert.assertEquals("0.1.0.buildQualifier", productConfiguration.getVersion());
    }

    private void assertFileExists(String relativePath, File dir) {
        Assert.assertTrue(new File(dir, relativePath).isFile());
    }

    @Test
    public void testPrepareBuildProductEmptyQualifier() throws Exception {
        File basedir = TestUtil.getBasedir("unitTestResources");
        File productFile = new File(basedir, "test.product");
        Product product = new Product(productFile);
        BuildOutputDirectory buildBasedir = new BuildOutputDirectory(new File(tempDir, "buildBasedir"));
        Product buildProduct = PublishProductMojo.prepareBuildProduct(product, buildBasedir, "");

        Assert.assertEquals(buildBasedir.getChild("products/testproduct/p2.inf"), buildProduct.getP2infFile());
        Assert.assertTrue(buildBasedir.getChild("products/testproduct/p2.inf").exists());

        ProductConfiguration productConfiguration = ProductConfiguration.read(buildProduct.getProductFile());
        Assert.assertEquals("0.1.0", productConfiguration.getVersion());
    }

    @Test
    public void testCopyMissingP2Inf() throws IOException {
        File productFile = new File(sourceDirectory, "test.product");
        productFile.createNewFile();

        File p2InfTarget = new File(targetDirectory, "p2.inf");
        PublishProductMojo.copyP2Inf(Product.getSourceP2InfFile(productFile), p2InfTarget);
        Assert.assertFalse(p2InfTarget.exists());
    }

    @Test
    public void testGetSourceP2InfFile() throws IOException {
        String p2InfFile = Product.getSourceP2InfFile(new File("./test/test.product")).getCanonicalPath();
        Assert.assertEquals(new File("./test/test.p2.inf").getCanonicalPath(), p2InfFile);
    }

    private File createTempDir(String prefix) throws IOException {
        File directory = File.createTempFile(prefix, "");
        if (directory.delete()) {
            directory.mkdirs();
            return directory;
        } else {
            throw new IOException("Could not create temp directory at: " + directory.getAbsolutePath());
        }
    }

}
