/*******************************************************************************
 * Copyright (c) 2016 SAP SE
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FeatureWithMultipleFiltersTest extends AbstractTychoIntegrationTest {

    @Test
    public void testFeaturePatch() throws Exception {
        Verifier verifier = getVerifier("feature.multiplefilters", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
