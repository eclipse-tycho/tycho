/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO300launcherIcons;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class LauncherIconsTest extends AbstractTychoIntegrationTest {
    @Test
    public void testEclipseSourceBundleManifestAttributes() throws Exception {
        Verifier verifier = getVerifier("/TYCHO300launcherIcons/product");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        // TODO is there a good way to test changes to native launcher?
    }

}
