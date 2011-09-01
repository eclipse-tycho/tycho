/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.test.util.InstallableUnitUtil;
import org.junit.Before;
import org.junit.Test;

public class ResolutionContextLocalRepositoryTest {

    private static final NullProgressMonitor NULL_MONITOR = new NullProgressMonitor();
    private MavenLoggerStub logger = new MavenLoggerStub();
    private ResolutionContextImpl context;
    private IInstallableUnit localIu;

    @Before
    public void setup() {
        File localRepository = new File("target/localrepo");
        IProvisioningAgent agent = P2ResolverFactoryImpl.getProvisioningAgent(localRepository, false);
        P2RepositoryCache repositoryCache = (P2RepositoryCache) agent.getService(P2RepositoryCache.SERVICE_NAME);
        URI uri = localRepository.toURI();
        RepositoryReader contentLocator = new LocalRepositoryReader(localRepository);
        TychoRepositoryIndex artifactsIndex = FileBasedTychoRepositoryIndex.createRepositoryIndex(localRepository,
                FileBasedTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
        TychoRepositoryIndex metadataIndex = FileBasedTychoRepositoryIndex.createRepositoryIndex(localRepository,
                FileBasedTychoRepositoryIndex.METADATA_INDEX_RELPATH);
        LocalArtifactRepository localArtifactRepository = new LocalArtifactRepository(localRepository, artifactsIndex,
                contentLocator);
        LocalMetadataRepository localMetadataRepository = new LocalMetadataRepository(uri, metadataIndex,
                contentLocator);
        repositoryCache.putRepository(uri, localMetadataRepository, localArtifactRepository);

        context = new P2ResolverFactoryImpl().createResolutionContext(localRepository, false, false, logger);

        localIu = InstallableUnitUtil.createIU("bundle", "1.0.0");
        localMetadataRepository.addInstallableUnit(localIu, new GAV("group", "bundle", "1.0.0"));

        final IQueryable<IInstallableUnit> ius = context.gatherAvailableInstallableUnits(NULL_MONITOR);
        final Set<IInstallableUnit> resolvedIus = ius.query(QueryUtil.ALL_UNITS, NULL_MONITOR).toSet();
        assertTrue(resolvedIus.contains(localIu));
    }

    @Test
    public void testWarnAboutLocalIu() {
        context.warnAboutLocalIus(Collections.singleton(localIu));
        assertEquals(Arrays.asList("The following locally built units have been used to resolve project dependencies:",
                "  bundle/1.0.0"), logger.getWarnings());
    }

    @Test
    public void testDontWarnAboutOtherIu() {
        context.warnAboutLocalIus(Collections.singleton(InstallableUnitUtil.createIU("bundle", "1.0.1")));
        assertTrue(logger.getWarnings().isEmpty());
    }

    @Test
    public void testDontWarnAboutEmptySet() {
        final Collection<IInstallableUnit> noIus = Collections.emptySet();
        context.warnAboutLocalIus(noIus);
        assertTrue(logger.getWarnings().isEmpty());
    }
}
