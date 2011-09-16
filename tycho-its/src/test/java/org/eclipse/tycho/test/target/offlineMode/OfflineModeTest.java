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
package org.eclipse.tycho.test.target.offlineMode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OfflineModeTest extends AbstractTychoIntegrationTest {

    private HttpServer server;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.startServer();
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        Verifier verifier = getVerifier("target.offlineMode", false);
        String url = server.addServer("test", new File(verifier.getBasedir(), "repo"));
        verifier.getSystemProperties().setProperty("p2.repo", url);

        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, url);

        verifier.setLogFileName("log-online.txt");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        assertFalse(server.getAccessedUrls("test").isEmpty());
        server.getAccessedUrls("test").clear();

        verifier.getCliOptions().add("--offline");
        verifier.setLogFileName("log-offline.txt");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        Set<String> urls = new LinkedHashSet<String>(server.getAccessedUrls("test"));
        assertTrue(urls.toString(), urls.isEmpty());
    }

}
