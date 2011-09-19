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
package org.eclipse.tycho.test.TYCHO300launcherIcons;

import org.apache.maven.it.Verifier;
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
