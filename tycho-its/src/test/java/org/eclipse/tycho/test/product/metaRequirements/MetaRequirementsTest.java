/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product.metaRequirements;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MetaRequirementsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProductInstallationWithCustomTouchpoint() throws Exception {
        /*
         * Project building a product distribution which includes a bundle that uses a custom
         * touchpoint. The implementation of the touchpoint is installed into the director building
         * the distribution through a p2 metaRequirement. This requires a p2 director which is
         * itself a p2-updatable installation. Therefore, a standalone p2 director is created in the
         * target folder before the actual product materialization. (Tycho's OSGi runtime, which
         * also includes a director, is intentionally not updatable and hence cannot be used.)
         */
        Verifier verifier = getVerifier("product.metaRequirements", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("The custom touchpoint action has been executed");
    }
}
