/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tycho.test.p2Repository;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DownloadVerifyNoDigestAlgoTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("p2Repository.downloadVerifyNoDigestAlgo", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifyTextNotInLog(verifier, "No digest algorithm is available to verify download");
    }
}
