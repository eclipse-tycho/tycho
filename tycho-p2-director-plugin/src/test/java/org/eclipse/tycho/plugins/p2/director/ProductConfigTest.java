/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.junit.Test;

public class ProductConfigTest {

    private ProductConfig subject;

    private List<DependencySeed> projectSeeds = new ArrayList<>();

    @Test
    public void testNoConfigAndNothingPublished() throws Exception {
        subject = new ProductConfig(null, projectSeeds);

        assertEquals(Collections.emptyList(), subject.getProducts());
    }

    @Test
    public void testNoConfigAndNoProductPublished() throws Exception {
        projectSeeds.add(otherSeed("feature.id", ArtifactType.TYPE_ECLIPSE_FEATURE));

        subject = new ProductConfig(null, projectSeeds);

        assertEquals(Collections.emptyList(), subject.getProducts());
    }

    @Test
    public void testNoConfigDefaultsToPublishedProducts() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));
        projectSeeds.add(otherSeed("feature.id", ArtifactType.TYPE_ECLIPSE_FEATURE));

        subject = new ProductConfig(null, projectSeeds);

        List<Product> products = subject.getProducts();
        assertEquals(2, products.size());
        assertTrue(products.contains(new Product("product.id.1")));
        assertTrue(products.contains(new Product("product.id.2")));
    }

    @Test
    public void testExplicitProduct() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));
        List<Product> userConfig = Collections.singletonList(new Product("product.id.1"));

        subject = new ProductConfig(userConfig, projectSeeds);

        List<Product> expected = Arrays.asList(new Product("product.id.1"));
        assertEquals(expected, subject.getProducts());
    }

    @Test(expected = MojoFailureException.class)
    public void testNonExistingExplicitProduct() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));
        List<Product> userConfig = Collections.singletonList(new Product("product.id.3"));

        subject = new ProductConfig(userConfig, projectSeeds);
    }

    @Test(expected = MojoFailureException.class)
    public void testProductWithoutId() throws Exception {
        List<Product> userConfig = Collections.singletonList(new Product());
        subject = new ProductConfig(userConfig, projectSeeds);
    }

    @Test
    public void testUniqueAttachIds() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));
        projectSeeds.add(productSeed("product.id.3"));
        List<Product> userConfig = Arrays.asList(new Product("product.id.2"), new Product("product.id.3", "extra"));

        subject = new ProductConfig(userConfig, projectSeeds);
        assertEquals(true, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateAttachId() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));

        subject = new ProductConfig(null, projectSeeds);
        assertEquals(false, subject.uniqueAttachIds());
    }

    @Test
    public void testDuplicateExplicitAttachId() throws Exception {
        projectSeeds.add(productSeed("product.id.1"));
        projectSeeds.add(productSeed("product.id.2"));
        projectSeeds.add(productSeed("product.id.3"));
        List<Product> userConfig = Arrays.asList(new Product("product.id.1", "attach"), new Product("product.id.2"),
                new Product("product.id.3", "attach"));

        subject = new ProductConfig(userConfig, projectSeeds);
        assertEquals(false, subject.uniqueAttachIds());
    }

    private static DependencySeed productSeed(String id) {
        InstallableUnitDescription desc = new InstallableUnitDescription();
        desc.setId(id);
        desc.setVersion(Version.parseVersion("1.0.0.20140207"));
        return new DependencySeed(ArtifactType.TYPE_ECLIPSE_PRODUCT, id, MetadataFactory.createInstallableUnit(desc),
                null);
    }

    private static DependencySeed otherSeed(String id, String type) {
        InstallableUnitDescription desc = new InstallableUnitDescription();
        desc.setId(id);
        desc.setVersion(Version.parseVersion("1.0.0.20140207"));
        return new DependencySeed(type, id, MetadataFactory.createInstallableUnit(desc), null);
    }

}
