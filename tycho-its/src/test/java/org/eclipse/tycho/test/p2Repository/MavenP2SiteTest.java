/*******************************************************************************
 * Copyright (c) 2021Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MavenP2SiteTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProduceConsume() throws Exception {
        {//parent
            Verifier verifier = getVerifier("p2mavensite", false);
            verifier.executeGoals(asList("install"));
            verifier.verifyErrorFreeLog();
        }
        { // producer
            Verifier verifier = getVerifier("p2mavensite/producer", false);
            verifier.executeGoals(asList("clean", "install"));
            verifier.verifyErrorFreeLog();
            assertTrue(new File(verifier.getBasedir(), "target/repository/artifacts.xml").exists());
            assertTrue(new File(verifier.getBasedir(), "target/repository/content.xml").exists());
            assertTrue(new File(verifier.getBasedir(), "target/p2-site.zip").exists());
        }
        { // consumer
            Verifier verifier = getVerifier("p2mavensite/consumer", false);
            verifier.executeGoals(asList("clean", "verify"));
            verifier.verifyErrorFreeLog();
        }
    }
}
