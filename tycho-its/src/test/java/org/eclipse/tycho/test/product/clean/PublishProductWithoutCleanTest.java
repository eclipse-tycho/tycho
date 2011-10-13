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
package org.eclipse.tycho.test.product.clean;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class PublishProductWithoutCleanTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProductUnitsAreCleaned() throws Exception {
        Verifier verifier = getVerifier("product.clean", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_342.toString());

        // run build to make target folder dirty
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        // there is a product IU in the the p2 repository in the target folder
        P2RepositoryTool p2Repository1 = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
        IU product1 = p2Repository1.getUniqueIU("product.uid");

        // now do a change to the product definition (here: update the version) ...
        File oldProductFile = new File(verifier.getBasedir(), "/minimal.product");
        File newProductFile = new File(verifier.getBasedir(), "/minimal.product_v2");
        assertTrue(oldProductFile.delete());
        assertTrue(newProductFile.renameTo(oldProductFile));

        // ... and rebuild without clean
        verifier.setAutoclean(false);
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        // expectation: there should be only one, new product IU - the old IU should no longer be in the repository
        P2RepositoryTool p2Repository2 = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
        IU product2 = p2Repository2.getUniqueIU("product.uid");
        assertTrue(product1.getVersion() != product2.getVersion());
    }
}
