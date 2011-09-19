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
package org.eclipse.tycho.test.TYCHO411targetLocationTypes;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TargetLocationTypesTest extends AbstractTychoIntegrationTest {
    @Test
    public void testMultiplatformReactorBuild() throws Exception {
        Verifier verifier = getVerifier("/TYCHO411targetLocationTypes", false);
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Target location type: Directory is not supported");
        verifier.verifyTextInLog("Target location type: Profile is not supported");
    }

}
