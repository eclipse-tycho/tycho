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
package org.eclipse.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.plugins.p2.director.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProductConfigTest {
    private File tempDir;

    private ProductConfig subject;

    @Before
    public void setUp() throws IOException {
        tempDir = createTempDir(getClass().getSimpleName());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testNoProductDefault() throws Exception {
        subject = new ProductConfig(null, tempDir);
        assertEquals(Collections.emptyList(), subject.getProducts());
    }

    @Test
    public void testProductDefault() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();

        subject = new ProductConfig(null, tempDir);

        List<Product> products = subject.getProducts();
        assertEquals(2, products.size());
        assertTrue(products.contains(new Product("product.id.1")));
        assertTrue(products.contains(new Product("product.id.2")));
    }

    @Test
    public void testExplicitProduct() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        List<Product> userConfig = Collections.singletonList(new Product("product.id.1"));

        subject = new ProductConfig(userConfig, tempDir);

        List<Product> expected = Arrays.asList(new Product("product.id.1"));
        assertEquals(expected, subject.getProducts());
    }

    @Test(expected = MojoFailureException.class)
    public void testNonExistingExplicitProduct() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        List<Product> userConfig = Collections.singletonList(new Product("product.id.3"));

        subject = new ProductConfig(userConfig, tempDir);
    }

    @Test(expected = MojoFailureException.class)
    public void testProductWithoutId() throws Exception {
        List<Product> userConfig = Collections.singletonList(new Product());
        subject = new ProductConfig(userConfig, tempDir);
    }

    @Test
    public void testUniqueAttachIds() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        new File(tempDir, "product.id.3").mkdirs();
        List<Product> userConfig = Arrays.asList(new Product("product.id.2"), new Product("product.id.3", "extra"));

        subject = new ProductConfig(userConfig, tempDir);
        assertEquals(true, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateAttachId() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();

        subject = new ProductConfig(null, tempDir);
        assertEquals(false, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateExplicitAttachId() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        new File(tempDir, "product.id.3").mkdirs();
        List<Product> userConfig = Arrays.asList(new Product("product.id.1", "attach"), new Product("product.id.2"),
                new Product("product.id.3", "attach"));

        subject = new ProductConfig(userConfig, tempDir);
        assertEquals(false, subject.uniqueAttachIds());
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
