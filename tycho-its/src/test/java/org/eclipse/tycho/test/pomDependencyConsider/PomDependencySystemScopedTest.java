/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_OXYGEN;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomDependencySystemScopedTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSystemScopedDependenciesIgnored() throws Exception {
        // project with pomDependency=consider and checked-in nested jar
        Verifier verifier = getVerifier("pomDependencyConsider.systemScope", false);
        verifier.getSystemProperties().setProperty("repo.url", ECLIPSE_OXYGEN.toString());
        // fails on second resolver invocation in TestMojo 
        // if (injected) system-scoped dependencies are not filtered out for pomDependency=consider  
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }
}
