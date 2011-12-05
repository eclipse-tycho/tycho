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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProductConfigTest {
    private File tempDir;
    private File projectDir;

    private ProductConfig subject;

    @Before
    public void setUp() throws IOException {
        tempDir = createTempDir(getClass().getSimpleName());
        projectDir = createTempDir(getClass().getSimpleName() + "projectDir");
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testNoProductDefault() throws Exception {
        subject = new ProductConfig(null, tempDir, projectDir);
        assertEquals(Collections.emptyList(), subject.getProducts());
    }

    @Test
    public void testProductDefault() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();

        subject = new ProductConfig(null, tempDir, projectDir);

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

        subject = new ProductConfig(userConfig, tempDir, projectDir);

        List<Product> expected = Arrays.asList(new Product("product.id.1"));
        assertEquals(expected, subject.getProducts());
    }

    @Test(expected = MojoFailureException.class)
    public void testNonExistingExplicitProduct() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        List<Product> userConfig = Collections.singletonList(new Product("product.id.3"));

        subject = new ProductConfig(userConfig, tempDir, projectDir);
    }

    @Test(expected = MojoFailureException.class)
    public void testProductWithoutId() throws Exception {
        List<Product> userConfig = Collections.singletonList(new Product());
        subject = new ProductConfig(userConfig, tempDir, projectDir);
    }

    @Test
    public void testUniqueAttachIds() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        new File(tempDir, "product.id.3").mkdirs();
        List<Product> userConfig = Arrays.asList(new Product("product.id.2"), new Product("product.id.3", "extra"));

        subject = new ProductConfig(userConfig, tempDir, projectDir);
        assertEquals(true, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateAttachId() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();

        subject = new ProductConfig(null, tempDir, projectDir);
        assertEquals(false, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateExplicitAttachId() throws Exception {
        new File(tempDir, "product.id.1").mkdirs();
        new File(tempDir, "product.id.2").mkdirs();
        new File(tempDir, "product.id.3").mkdirs();
        List<Product> userConfig = Arrays.asList(new Product("product.id.1", "attach"), new Product("product.id.2"),
                new Product("product.id.3", "attach"));

        subject = new ProductConfig(userConfig, tempDir, projectDir);
        assertEquals(false, subject.uniqueAttachIds());
    }

    @Test
    public void testNotExistingProductsDir() throws Exception {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=356716
        subject = new ProductConfig(null, new File(tempDir, "aNotExstistingFolder"), projectDir);
        assertTrue(subject.getProducts().isEmpty());
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
