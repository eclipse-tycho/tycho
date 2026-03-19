/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.surefire;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// regression test for TYCHO-380
// TODO is this case really tested? The target platform only a single JUnit version!
public class TwoJunitVersionsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("surefire.twoJunitVersions");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertFileExists(new File(verifier.getBasedir()), "target/surefire-reports/some.Test.txt");
    }
}
