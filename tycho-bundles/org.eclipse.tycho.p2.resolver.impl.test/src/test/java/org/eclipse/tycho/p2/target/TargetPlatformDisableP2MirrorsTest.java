package org.eclipse.tycho.p2.target;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.impl.resolver.P2RepositoryCache;
import org.eclipse.tycho.p2.impl.resolver.P2ResolverFactoryImpl;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Before;
import org.junit.Test;

public class TargetPlatformDisableP2MirrorsTest {

    private P2RepositoryCache repositoryCache;
    private File localRepo;

    @Before
    public void setUp() {
        localRepo = new File("target/localrepo");
        IProvisioningAgent agent = P2ResolverFactoryImpl.getProvisioningAgent(localRepo, false, new MavenLoggerStub());
        repositoryCache = (P2RepositoryCache) agent.getService(P2RepositoryCache.SERVICE_NAME);
        assertNotNull(repositoryCache);
    }

    @Test
    public void testDisableP2Mirrors() throws Exception {
        TargetPlatformBuilderImpl context = createResolutionContext(true);

        URI location = ResourceUtil.resourceFile("p2-mirrors-disable/disablemirrors").toURI();
        context.addP2Repository(location);
        assertNull(getP2MirrorsUrlFromCachedRepository(location));
    }

    @Test
    public void testWithoutDisableP2Mirrors() throws Exception {
        TargetPlatformBuilderImpl context = createResolutionContext(false);

        // need a different URI here, to force reloading
        // TODO why is the cache active in this test -> test through the right interface?
        URI location = ResourceUtil.resourceFile("p2-mirrors-disable/usemirrors").toURI();
        context.addP2Repository(location);
        assertNotNull(getP2MirrorsUrlFromCachedRepository(location));
    }

    private TargetPlatformBuilderImpl createResolutionContext(boolean disableP2Mirrors) {
        P2ResolverFactoryImpl p2ResolverFactoryImpl = new P2ResolverFactoryImpl();
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setOffline(false);
        mavenContext.setLocalRepositoryRoot(localRepo);
        mavenContext.setLogger(new MavenLoggerStub());
        p2ResolverFactoryImpl.setMavenContext(mavenContext);
        p2ResolverFactoryImpl.setLocalRepositoryIndices(createLocalRepoIndices(mavenContext));
        TargetPlatformBuilderImpl context = p2ResolverFactoryImpl.createTargetPlatformBuilder(null, disableP2Mirrors);
        return context;
    }

    private LocalRepositoryP2Indices createLocalRepoIndices(MavenContextImpl mavenContext) {
        LocalRepositoryP2IndicesImpl localRepoIndices = new LocalRepositoryP2IndicesImpl();
        localRepoIndices.setMavenContext(mavenContext);
        localRepoIndices.setFileLockService(new NoopFileLockService());
        return localRepoIndices;
    }

    private String getP2MirrorsUrlFromCachedRepository(URI location) {
        String p2MirrorsUrl = repositoryCache.getArtifactRepository(location).getProperty(
                AbstractArtifactRepository.PROP_MIRRORS_URL);
        return p2MirrorsUrl;
    }
}
