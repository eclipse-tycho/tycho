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

// TODO are there more cases?
public class BundleBuildOrderTest extends BuildOrderUnitTestBase {

    private TychoBuildOrderParticipant subject;

    @Before
    public void setupSubject() {
        subject = new TychoBuildOrderParticipant(new DefaultBundleReader());
    }

    @Test
    public void testBundleRelations() {
        File baseDir = getProject("bundle");

        subject.collectBundleRelations(exports, imports, baseDir);

        assertThat(exports, hasItem(bundleProvide("bundle.symbolic.name")));
        assertThat(exports, hasItem(packageExport("package.export.01")));
        assertThat(exports, hasItem(packageExport("package.export.02")));

        assertThat(imports, hasItem(bundleRequire("bundle.require.01")));
        assertThat(imports, hasItem(bundleRequire("bundle.require.02")));
        assertThat(imports, hasItem(packageImport("package.import.01")));
        assertThat(imports, hasItem(packageImport("package.import.02")));
        assertThat(imports, hasItem(packageImport("package.import.03")));
    }

    @Test
    public void testStandaloneBundleRelations() {
        File baseDir = getProject("bundle.standalone");

        subject.collectBundleRelations(exports, imports, baseDir);

        assertThat(exports.size(), is(1));
        assertThat(exports, hasItem(bundleProvide("standalone.bundle")));

        assertThat(imports.size(), is(0));
    }

    @Test
    public void testFragmentRelations() {
        File baseDir = getProject("fragment");

        subject.collectBundleRelations(exports, imports, baseDir);

        assertThat(exports.size(), is(1));
        assertThat(exports, hasItem(bundleProvide("fragment")));

        assertThat(imports, hasItem(bundleRequire("dep")));
        assertThat(imports, hasItem(bundleRequire("host")));
    }

}
