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
package org.eclipse.tycho.test.TYCHO319passwordProtectedP2Repository;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.Target;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
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
        String url = server.addServer("foo", new File("repositories/e342"));

        Verifier verifier = getVerifier("/TYCHO319passwordProtectedP2Repository", false, new File(
                "projects/TYCHO319passwordProtectedP2Repository/settings.xml"));
        verifier.getCliOptions().add("-P=repository");
        verifier.executeGoals(Arrays.asList("package", "-Dp2.repo=" + url));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testTargetDefinition() throws Exception {
        String url = server.addServer("foo", new File("repositories/e342"));

        Verifier verifier = getVerifier("/TYCHO319passwordProtectedP2Repository", false, new File(
                "projects/TYCHO319passwordProtectedP2Repository/settings.xml"));

        File platformFile = new File(verifier.getBasedir(), "platform.target");
        Target platform = Target.read(platformFile);
        platform.getLocations().get(0).getRepositories().get(0).setLocation(url);
        Target.write(platform, platformFile);

        verifier.getCliOptions().add("-P=target-definition");
        verifier.executeGoals(Arrays.asList("package", "-Dp2.repo=" + url));
        verifier.verifyErrorFreeLog();
    }
}
