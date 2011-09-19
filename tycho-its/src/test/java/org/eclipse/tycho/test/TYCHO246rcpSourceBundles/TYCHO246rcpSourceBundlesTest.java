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
package org.eclipse.tycho.test.TYCHO246rcpSourceBundles;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO246rcpSourceBundlesTest extends AbstractTychoIntegrationTest {
    @Test
    public void testMultiplatformReactorBuild() throws Exception {
        Verifier verifier = getVerifier("/TYCHO246rcpSourceBundles");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File productTarget = new File(verifier.getBasedir(), "product/target");
        assertFileExists(productTarget, "product/eclipse/plugins/org.eclipse.osgi_*.jar");
        assertFileDoesNotExist(productTarget, "product/eclipse/plugins/org.eclipse.osgi.source_*.jar");

        File productWithSourcesTarget = new File(verifier.getBasedir(), "productWithSources/target");
        assertFileExists(productWithSourcesTarget, "product/eclipse/plugins/org.eclipse.osgi_*.jar");
        assertFileExists(productWithSourcesTarget, "product/eclipse/plugins/org.eclipse.osgi.source_*.jar");
    }

}
