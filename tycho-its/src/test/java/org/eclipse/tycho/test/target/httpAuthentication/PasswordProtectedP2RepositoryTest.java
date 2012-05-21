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
package org.eclipse.tycho.test.target.httpAuthentication;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class PasswordProtectedP2RepositoryTest extends AbstractTychoIntegrationTest {

    private HttpServer server;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.startServer("test-user", "test-password");
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void testRepository() throws Exception {
        String url = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/e342"));

        Verifier verifier = getVerifier("target.httpAuthentication", false, new File(
                "projects/target.httpAuthentication/settings.xml"));
        verifier.getCliOptions().add("-P=repository");
        verifier.executeGoals(Arrays.asList("package", "-Dp2.repo=" + url));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testTargetDefinition() throws Exception {
        String url = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/e342"));

        Verifier verifier = getVerifier("target.httpAuthentication", false, new File(
                "projects/target.httpAuthentication/settings.xml"));

        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, url);

        verifier.getCliOptions().add("-P=target-definition");
        verifier.executeGoals(Arrays.asList("package", "-Dp2.repo=" + url));
        verifier.verifyErrorFreeLog();
    }
}
