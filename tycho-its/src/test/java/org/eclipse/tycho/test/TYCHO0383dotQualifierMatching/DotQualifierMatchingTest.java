/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0383dotQualifierMatching;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class DotQualifierMatchingTest extends AbstractTychoIntegrationTest {
    @Test
    public void testFeature() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0383dotQualifierMatching/featureDotQualifier", false);
        verifier.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertFileExists(new File(verifier.getBasedir()),
                "target/site/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");
    }

    @Test
    public void testProduct() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0383dotQualifierMatching/productDotQualifier", false);
        verifier.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertFileExists(new File(verifier.getBasedir()),
                "productDotQualifier.product/target/linux.gtk.x86_64/eclipse/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");
    }

}
