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

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SystemPropertiesTest extends AbstractTychoIntegrationTest {

    @Test
    public void exportProduct() throws Exception {
        // project that passes system property to forked VM -> supported since TYCHO-351
        Verifier verifier = getVerifier("surefire.systemProperties");
        verifier.executeGoal("integration-test");

        // assertion is done in eclipse-test-plugin
        verifier.verifyErrorFreeLog();
    }

}
