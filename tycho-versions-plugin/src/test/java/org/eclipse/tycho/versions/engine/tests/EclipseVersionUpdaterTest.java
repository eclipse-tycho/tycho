/*******************************************************************************
 * Copyright (c) 2011, 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - Bug #512326 Support product file names other than artifact id
 *    Bachmann electronic GmbH. - #517664 Support for updating p2iu versions
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertBundleManifest;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertFeatureXml;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertP2IuXml;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertPom;
import static org.eclipse.tycho.versions.engine.tests.ExtraAssertions.assertProductFile;

import java.io.File;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.tycho.testing.TestUtil;
import org.eclipse.tycho.versions.engine.EclipseVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.junit.jupiter.api.Test;

@PlexusTest
public class EclipseVersionUpdaterTest {

    @Inject
    private ProjectMetadataReader reader;

    @Inject
    private EclipseVersionUpdater updater;

    @Test
    public void test() throws Exception {
        File basedir = TestUtil.getBasedir("projects/updateeclipse");
        reader.addBasedir(basedir, true);

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

        assertPom(new File(basedir, "iu"));
        assertP2IuXml(new File(basedir, ("iu")));

    }
}
