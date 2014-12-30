/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.model.ProductConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PublishProductMojoUnitTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private final File testResources = new File("src/test/resources/unitTestResources");

    @Test
    public void testExtractRootFeatures() throws Exception {
        ProductConfiguration product = ProductConfiguration.read(new File(testResources, "rootFeatures.product"));
        List<DependencySeed> seeds = new ArrayList<DependencySeed>();

        Set<String> rootFeatures = PublishProductMojo.extractRootFeatures(product, seeds);

        // workaround for 428889: remove from root features so that p2 doesn't include them in the product IU
        assertThat(rootFeatures, not(hasItem("org.eclipse.rcp")));
        assertThat(rootFeatures, hasItem("org.eclipse.help"));
        assertThat(rootFeatures, hasItem("org.eclipse.egit"));

        assertThat(seeds.size(), not(is(0)));
        assertThat(seeds.get(0).getId(), is("org.eclipse.help"));
        assertThat(seeds.get(1).getId(), is("org.eclipse.egit"));
        assertThat(seeds.size(), is(2));
    }

}
