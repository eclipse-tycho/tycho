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
package org.eclipse.tycho.test.TYCHO253extraClassPathEntries;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExtraClassPathEntriesTest extends AbstractTychoIntegrationTest {
    @Test
    public void testJarsExtraClasspath() throws Exception {
        Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest1");
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testExtraClasspath() throws Exception {
        Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest2");
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testReferenceToInnerJar() throws Exception {
        Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries");
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }
}
