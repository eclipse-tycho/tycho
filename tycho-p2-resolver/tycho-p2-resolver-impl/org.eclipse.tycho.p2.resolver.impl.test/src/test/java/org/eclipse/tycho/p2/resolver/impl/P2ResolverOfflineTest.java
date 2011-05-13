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
package org.eclipse.tycho.p2.resolver.impl;

import static org.eclipse.tycho.p2.resolver.impl.P2ResolverTest.addMavenProject;
import static org.eclipse.tycho.p2.resolver.impl.P2ResolverTest.getEnvironments;
import static org.eclipse.tycho.p2.resolver.impl.P2ResolverTest.getLocalRepositoryLocation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.P2ResolverImpl;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.impl.test.P2RepositoryCacheImpl;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;
import org.eclipse.tycho.test.util.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverOfflineTest {

    private HttpServer server;
    private String servedUrl;
    private ResolutionContext context;
    private P2Resolver impl;

    @Before
    public void initResolver() {
        MavenLogger logger = new MavenLoggerStub();
        context = new ResolutionContextImpl(logger);
        impl = new P2ResolverImpl(logger);
    }

    @Before
    public void startHttpServer() throws Exception {
        server = HttpServer.startServer();
        servedUrl = server.addServer("e342", new File("resources/repositories/e342"));
    }

    @After
    public void stopHttpServer() throws Exception {
        HttpServer _server = server;
        server = null;
        if (_server != null) {
            _server.stop();
        }
    }

    private List<P2ResolutionResult> resolveFromHttp(ResolutionContext context, P2Resolver impl, String url)
            throws IOException, URISyntaxException {
        context.setRepositoryCache(new P2RepositoryCacheImpl());
        context.setLocalRepositoryLocation(getLocalRepositoryLocation());
        context.addP2Repository(new URI(url));

        impl.setEnvironments(getEnvironments());

        String groupId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        File bundle = new File("resources/resolver/bundle01").getCanonicalFile();

        addMavenProject(context, bundle, P2Resolver.TYPE_ECLIPSE_PLUGIN, groupId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);
        return results;
    }

    @Test
    public void offline() throws Exception {

        // prime local repository
        resolveFromHttp(context, impl, servedUrl);

        // now go offline and resolve again
        context = new ResolutionContextImpl(new MavenLoggerStub());
        context.setOffline(true);
        List<P2ResolutionResult> results = resolveFromHttp(context, impl, servedUrl);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());
    }

    @Test
    public void offlineNoLocalCache() throws Exception {
        delete(getLocalRepositoryLocation());

        context.setOffline(true);

        try {
            resolveFromHttp(context, impl, servedUrl);
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            // TODO better assertion
        }
    }

    static void delete(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }

        if (dir.isDirectory()) {
            File[] members = dir.listFiles();
            if (members != null) {
                for (File member : members) {
                    delete(member);
                }
            }
        }

        Assert.assertTrue("Delete " + dir.getAbsolutePath(), dir.delete());
    }
}
