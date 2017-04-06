/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronics GmbH. - Bug #512326 Support product file names other than artifact id
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import java.io.File;

import org.eclipse.tycho.testing.TestUtil;
import org.eclipse.tycho.versions.engine.EclipseVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

public class EclipseVersionUpdaterTest extends AbstractVersionChangeTest {

    private ProjectMetadataReader reader;

    @Override
    protected void setUp() throws Exception {
        reader = lookup(ProjectMetadataReader.class);
    }

    public void test() throws Exception {
        File basedir = TestUtil.getBasedir("projects/updateeclipse");

        reader.addBasedir(basedir);

        EclipseVersionUpdater updater = lookup(EclipseVersionUpdater.class);
        updater.setProjects(reader.getProjects());
        updater.apply();

        assertPom(new File(basedir, "bundle"));
        assertBundleManifest(new File(basedir, "bundle"));

        assertPom(new File(basedir, "feature"));
        assertFeatureXml(new File(basedir, "feature"));

        assertPom(new File(basedir, "product"));
        assertProductFile(new File(basedir, "product"), "product.product");

        assertPom(new File(basedir, "repository"));
        assertProductFile(new File(basedir, "repository"), "repository.product");

        assertPom(new File(basedir, "repositoryWithOneProductFile"));
        assertProductFile(new File(basedir, "repositoryWithOneProductFile"), "anotherNameThanArtifactId.product");

        assertPom(new File(basedir, "repositoryWith2ProductFiles"));
        assertProductFile(new File(basedir, "repositoryWith2ProductFiles"), "aProductFile.product");
        assertProductFile(new File(basedir, "repositoryWith2ProductFiles"), "anotherProductFile.product");

    }
}
