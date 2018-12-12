/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class RequireBundleSurefireTest extends AbstractTychoIntegrationTest {

    // requested in bug 485926
    @Test
    public void loadResourceFromRequireBundle() throws Exception {
        Verifier verifier = getVerifier("surefire.requirebundle/org.eclipse.tycho.build");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
