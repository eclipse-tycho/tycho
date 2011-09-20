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
package org.eclipse.tycho.p2.impl.resolver;

import static org.eclipse.tycho.p2.impl.resolver.P2ResolverTest.getLocalRepositoryLocation;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverOfflineTest extends P2ResolverTestBase {
    private static final boolean DISABLE_MIRRORS = false;

    private HttpServer server;
    private String servedUrl;

    @Before
    public void initResolver() throws Exception {
        P2ResolverFactoryImpl p2ResolverFactoryImpl = createP2ResolverFactory(false);
        context = p2ResolverFactoryImpl.createResolutionContext(null, DISABLE_MIRRORS);
        impl = new P2ResolverImpl(new MavenLoggerStub());
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
        context.addP2Repository(new URI(url));

        impl.setEnvironments(getEnvironments());

        String id = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        File bundle = ResourceUtil.resourceFile("resolver/bundle01");
        addReactorProject(bundle, P2Resolver.TYPE_ECLIPSE_PLUGIN, id);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);
        return results;
    }

    @Test
    public void offline() throws Exception {

        // prime local repository
        resolveFromHttp(context, impl, servedUrl);

        // now go offline and resolve again
        P2ResolverFactoryImpl p2ResolverFactory = new P2ResolverFactoryImpl();
        MavenContext mavenContext = createMavenContext(true, new MavenLoggerStub());
        p2ResolverFactory.setMavenContext(mavenContext);
        p2ResolverFactory.setLocalRepositoryIndices(createLocalRepoIndices(mavenContext));
        context = p2ResolverFactory.createResolutionContext(null, DISABLE_MIRRORS);
        List<P2ResolutionResult> results = resolveFromHttp(context, impl, servedUrl);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());
    }

    @Test
    public void offlineNoLocalCache() throws Exception {
        delete(getLocalRepositoryLocation());

        boolean offline = true;
        P2ResolverFactoryImpl p2ResolverFactory = createP2ResolverFactory(offline);
        context = p2ResolverFactory.createResolutionContext(null, DISABLE_MIRRORS);

        try {
            resolveFromHttp(context, impl, servedUrl);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getCause(), is(ProvisionException.class));
            assertThat(e.getMessage(), containsString("offline"));
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
