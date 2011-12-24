/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import java.io.File;

import org.eclipse.tycho.testing.TestUtil;
import org.eclipse.tycho.versions.engine.PomVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

public class PomUpdaterTest extends AbstractVersionChangeTest {

    public void test() throws Exception {
        File basedir = TestUtil.getBasedir("projects/updatepom");

        ProjectMetadataReader reader = new ProjectMetadataReader();
        reader.addBasedir(basedir);

        PomVersionUpdater updater = lookup(PomVersionUpdater.class);
        updater.setProjects(reader.getProjects());
        updater.apply();

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "feature"));
        assertFeatureXml(new File(basedir, "feature"));

        assertPom(new File(basedir, "product"));
        assertProductFile(new File(basedir, "product"), "product.product");
    }
}
