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
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;

import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.junit.Before;
import org.junit.Test;

public class FeatureBuildOrderTest extends BuildOrderUnitTestBase {

    private TychoBuildOrderParticipant subject;

    @Before
    public void setupSubject() {
        subject = new TychoBuildOrderParticipant(new DefaultBundleReader());
    }

    @Test
    public void testEmptyFeature() {
        File baseDir = getProject("feature.empty");

        subject.collectFeatureRelations(exports, imports, baseDir);

        assertThat(exports, hasItem(featureProvide("feature.id")));
        assertThat(imports.size(), is(0));
    }

    @Test
    public void testFeatureImports() {
        File baseDir = getProject("feature");

        subject.collectFeatureRelations(exports, imports, baseDir);

        assertThat(imports, hasItem(bundleRequire("included.bundle")));
        assertThat(imports, hasItem(bundleRequire("included.fragment")));
        assertThat(imports, hasItem(bundleRequire("included.fragment.withFilter"))); // filters are ignored for the build order

        assertThat(imports, hasItem(featureRequire("included.feature.a")));
        assertThat(imports, hasItem(featureRequire("included.feature.b")));

        assertThat(imports, hasItem(bundleRequire("required.bundle.1")));
        assertThat(imports, hasItem(bundleRequire("required.bundle.2")));
        assertThat(imports, hasItem(featureRequire("required.feature")));
    }
}
