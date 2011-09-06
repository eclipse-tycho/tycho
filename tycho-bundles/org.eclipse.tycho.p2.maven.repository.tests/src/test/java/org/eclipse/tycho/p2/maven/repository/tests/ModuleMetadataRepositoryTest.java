/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.ModuleMetadataRepository;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class ModuleMetadataRepositoryTest {

    private static final IVersionedId BUNDLE_UNIT = new VersionedId("bundle", "1.2.3.201011101425");

    private static final IVersionedId SOURCE_UNIT = new VersionedId("bundle.source", "1.2.3.TAGNAME");

    private static File moduleDir;

    private File tempDir = null;

    private ModuleMetadataRepository subject;

    @BeforeClass
    public static void init() throws Exception {
        moduleDir = new File("resources/repositories/module/target").getAbsoluteFile();
    }

    @SuppressWarnings("restriction")
    @After
    public void cleanUp() {
        if (tempDir != null)
            FileUtils.deleteAll(tempDir);
        tempDir = null;
    }

    @Test
    public void testLoadRepository() throws Exception {
        subject = new ModuleMetadataRepository(null, moduleDir);

        assertQueryForUnit(subject, BUNDLE_UNIT);
        assertQueryForUnit(subject, SOURCE_UNIT);
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        tempDir = createTempDir();
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempDir.toURI());
        IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);

        IMetadataRepository subject = repoManager.loadRepository(moduleDir.toURI(), null);

        assertQueryForUnit(subject, BUNDLE_UNIT);
        assertQueryForUnit(subject, SOURCE_UNIT);
    }

    private static void assertQueryForUnit(IMetadataRepository subject, IVersionedId unitIdentifier) {
        IQueryResult<IInstallableUnit> result = subject.query(QueryUtil.createIUQuery(unitIdentifier.getId()), null);
        Iterator<IInstallableUnit> iterator = result.iterator();
        assertEquals(true, iterator.hasNext());
        IInstallableUnit unit = iterator.next();
        assertEquals(false, iterator.hasNext());
        assertEquals(unitIdentifier.getVersion(), unit.getVersion());
    }

    private static File createTempDir() throws IOException {
        return ModuleArtifactRepositoryTest.createTempDir(ModuleMetadataRepositoryTest.class);
    }
}
