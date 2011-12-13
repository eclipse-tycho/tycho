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
package org.eclipse.tycho.test.bug351842_optionalDependencies;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class OptionalDependenciesTest extends AbstractTychoIntegrationTest {

    @Test
    public void requireBundle() throws Exception {
        Verifier verifier = getVerifier("/351842_optionalDependencies/require-bundle", false);
        verifier.getCliOptions().add("-De342-repo=" + P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void requireBundleIgnore() throws Exception {
        Verifier verifier = getVerifier("/351842_optionalDependencies/require-bundle-ignore", false);
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }
}
