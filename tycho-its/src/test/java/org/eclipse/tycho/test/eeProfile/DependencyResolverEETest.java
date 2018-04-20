/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

// tests that the dependency resolver resolves for the configured execution environment (bug 364095)
@SuppressWarnings("unchecked")
public class DependencyResolverEETest extends AbstractTychoIntegrationTest {

    @Test
    public void eeFromBREE() throws Exception {
        Verifier verifier = getVerifier("/eeProfile/ee-from-bree", false);
        verifier.getCliOptions().add(
                "-Djavax.xml-repo=" + ResourceUtil.resolveTestResource("repositories/javax.xml").toURI().toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void eeFromPOM() throws Exception {
        Verifier verifier = getVerifier("/eeProfile/ee-from-pom", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void breeForDependencyHigherThanCurrentBREE() throws Exception {
        Verifier verifier = getVerifier("/eeProfile/dependencyHigherBREE", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

}
