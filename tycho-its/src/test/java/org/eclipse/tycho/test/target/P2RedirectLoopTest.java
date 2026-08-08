/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import java.io.File;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that a permanent (HTTP 301) redirect loop in a p2 repository URL is
 * detected and reported with a clear warning instead of causing a
 * StackOverflowError.
 *
 * @see <a href="https://github.com/eclipse-tycho/tycho/issues/4451">issue #4451</a>
 */
public class P2RedirectLoopTest extends AbstractTychoIntegrationTest {

    private HttpServer server;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.startServer();
    }

    @After
    public void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testRedirectLoopIsCaughtWithWarning() throws Exception {
        String redirectLoopUrl = server.addPermanentRedirect("redirect-loop",
                path -> server.getUrl("redirect-loop") + path);

        Verifier verifier = getVerifier("target.redirectLoop", false);
        File targetFile = new File(verifier.getBasedir(), "targetplatform.target");
        TargetDefinitionUtil.setRepositoryURLs(targetFile, redirectLoopUrl);

        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("verify"));
        Assert.assertFalse("Expected the redirect-loop server to have been accessed",
                server.getAccessedUrls("redirect-loop").isEmpty());
        verifier.verifyTextInLog("would redirect to itself, this would cause a loop");
    }

}
