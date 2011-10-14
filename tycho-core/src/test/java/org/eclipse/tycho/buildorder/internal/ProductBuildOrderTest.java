/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

// TODO add more tests
public class ProductBuildOrderTest extends BuildOrderUnitTestBase {

    private TychoBuildOrderParticipant subject;

    @Before
    public void setupSubject() {
        subject = new TychoBuildOrderParticipant(null);
    }

    @Test
    public void testExportsOfProduct() {
        File baseDir = getProject("product.featureBased");

        subject.collectProductRelations(exports, imports, baseDir);

        assertThat(exports.size(), is(0)); // TODO export IU
    }

    @Test
    public void testImportsOfFeatureBasedProduct() {
        File baseDir = getProject("product.featureBased");

        subject.collectProductRelations(exports, imports, baseDir);

        assertThat(imports, hasItem(featureRequire("feature.include.1")));
        assertThat(imports, hasItem(featureRequire("feature.include.2")));
        assertThat(imports, not(hasItem(bundleRequire("bundle.toBeIgnored"))));
    }

    @Test
    public void testImportsOfPluginBasedProduct() {
        File baseDir = getProject("product.pluginBased");

        subject.collectProductRelations(exports, imports, baseDir);

        assertThat(imports, hasItem(bundleRequire("bundle.include.1")));
        assertThat(imports, hasItem(bundleRequire("bundle.include.2")));
        assertThat(imports, not(hasItem(featureRequire("feature.toBeIgnored"))));
    }

}
