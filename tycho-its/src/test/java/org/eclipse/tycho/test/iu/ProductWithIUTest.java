/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.iu;

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductWithIUTest extends AbstractTychoIntegrationTest {

    @Test
    public void testRootFilesFromIUPackagingInstalledAndInRepo() throws Exception {
        Verifier verifier = getVerifier("iu.product", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_OXYGEN.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File rootFileInstalledByIU = new File(verifier.getBasedir(),
                "eclipse-repository/target/products/main.product.id/linux/gtk/x86/myFile.txt");
        assertThat(rootFileInstalledByIU, isFile());

        P2RepositoryTool p2Repository = P2RepositoryTool
                .forEclipseRepositoryModule(new File(verifier.getBasedir(), "eclipse-repository"));
        assertThat(p2Repository.findBinaryArtifact("iup.iuForRootFile"), isFile());

        assertThat(p2Repository.getAllUnitIds(), hasItem("iup.iuForRootFile"));
    }

}
