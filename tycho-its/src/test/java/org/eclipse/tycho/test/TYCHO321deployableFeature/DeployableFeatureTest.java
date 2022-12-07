/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO321deployableFeature;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class DeployableFeatureTest extends AbstractTychoIntegrationTest {

    @Test
    public void testDeployableFeature() throws Exception {
        Verifier v01 = getVerifier("TYCHO321deployableFeature");
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();

        File site = new File(v01.getBasedir(), "target/site");
        Assert.assertTrue(site.isDirectory());

        Assert.assertTrue(new File(site, "features").list().length > 0);
        Assert.assertTrue(new File(site, "plugins").list().length > 0);

        Assert.assertTrue(new File(site, "artifacts.jar").isFile());
        Assert.assertTrue(new File(site, "content.jar").isFile());
    }

}
