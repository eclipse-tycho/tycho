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
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

// regression test for TYCHO-282
// TODO does this test do anything more than other tests?
public class ImplicitTestDependenciesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testLocalMavenRepository() throws Exception {
        Verifier v01 = getVerifier("surefire.implicitDeps", false);
        v01.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_342.toString());
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();
    }

}
