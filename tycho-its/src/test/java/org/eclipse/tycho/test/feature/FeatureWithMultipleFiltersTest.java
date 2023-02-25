/*******************************************************************************
 * Copyright (c) 2016 SAP SE
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import org.apache.maven.shared.verifier.Verifier;
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
