/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.fragment;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class SystemFragmentTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSystemFragmentBuild() throws Exception {
        Verifier verifier = getVerifier("fragment.systemBundle/servletbridge", false);
        verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_352);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        // TODO also test that the system bundle fragment can be used?
    }

}
