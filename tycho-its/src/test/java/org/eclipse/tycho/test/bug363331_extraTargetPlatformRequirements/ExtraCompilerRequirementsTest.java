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
package org.eclipse.tycho.test.bug363331_extraTargetPlatformRequirements;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExtraCompilerRequirementsTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        // TODO this is an artificial test case - find a test closer to a real use case
        // TODO avoid remote repositories
        Verifier verifier = getVerifier("/363331_extraTargetPlatformRequirements", false);
//        verifier.getCliOptions().add("-De342-repo=" + P2Repositories.ECLIPSE_342.toString());
//        verifier.getCliOptions().add("-De352-repo=" + P2Repositories.ECLIPSE_352.toString());
        verifier.executeGoals(Arrays.asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }
}
