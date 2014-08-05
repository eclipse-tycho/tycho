/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.iu;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ArchiveContentUtil;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductWithIUTest extends AbstractTychoIntegrationTest {

    @Test
    public void testFileInRootFolder() throws Exception {
        Verifier verifier = getVerifier("iu.product", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_KEPLER.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File rootFileInstalledByIU = new File(verifier.getBasedir(),
                "eclipse-repository/target/products/main.product.id/linux/gtk/x86/myFile.txt");
        assertThat(rootFileInstalledByIU.exists(), is(true));

        File artifactInRepo = new File(verifier.getBasedir(),
                "eclipse-repository/target/repository/binary/iuForRootFile_1.0.0");
        assertThat(artifactInRepo.exists(), is(true));

        File metadataRepo = new File(verifier.getBasedir(), "eclipse-repository/target/repository/content.jar");
        assertThat(ArchiveContentUtil.getFileContent(metadataRepo, "content.xml"), containsString("id='iuForRootFile'"));
    }
}
