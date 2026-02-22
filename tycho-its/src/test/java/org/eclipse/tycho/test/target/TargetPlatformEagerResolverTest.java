/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import java.util.List;

// See issue https://github.com/eclipse-tycho/tycho/issues/4653
public class TargetPlatformEagerResolverTest extends AbstractTychoIntegrationTest {
    @Test
    public void testTargetPlatformForJUnit5() throws Exception {
        Verifier verifier = getVerifier("target.eagerResolver", false, true);
        verifier.executeGoals(List.of("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }
}
