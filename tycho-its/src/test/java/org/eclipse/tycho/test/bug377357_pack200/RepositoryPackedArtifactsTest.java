/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug377357_pack200;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class RepositoryPackedArtifactsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/377357_pack200", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        assertDirectory(new File(basedir, "product.pack200/target/repository/plugins/"), "bundle_1.0.0.123abc.jar",
                "bundle_1.0.0.123abc.jar.pack.gz", "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar",
                "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar.pack.gz");

        assertDirectory(new File(basedir, "product.nopack200/target/repository/plugins/"), "bundle_1.0.0.123abc.jar",
                "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");

        // TODO verify metadata contains packed artifacts
    }

    private void assertDirectory(File dir, String... expectedFiles) {
        Set<String> actualFiles = new HashSet<String>(Arrays.asList(dir.list()));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(expectedFiles)), actualFiles);
    }
}
