/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.ModuleMetadataRepository;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModuleMetadataRepositoryTest {

    private static final IVersionedId BUNDLE_UNIT = new VersionedId("bundle", "1.2.3.201011101425");
    private static final IVersionedId SOURCE_UNIT = new VersionedId("bundle.source", "1.2.3.TAGNAME");

    private static File moduleDir;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private IMetadataRepository subject;

    @BeforeClass
    public static void init() throws Exception {
        moduleDir = new File("resources/repositories/module/target").getAbsoluteFile();
    }

    @Test
    public void testLoadRepository() throws Exception {
        subject = new ModuleMetadataRepository(null, moduleDir);

        assertThat(unitsIn(subject), hasItem(BUNDLE_UNIT));
        assertThat(unitsIn(subject), hasItem(SOURCE_UNIT));
    }

    @Test
    public void testLoadRepositoryWithFactory() throws Exception {
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempFolder.newFolder("agent").toURI());
        IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);

        subject = repoManager.loadRepository(moduleDir.toURI(), null);

        assertThat(unitsIn(subject), hasItem(BUNDLE_UNIT));
        assertThat(unitsIn(subject), hasItem(SOURCE_UNIT));
    }

    private static List<IVersionedId> unitsIn(IMetadataRepository repo) {
        IQueryResult<IInstallableUnit> units = repo.query(QueryUtil.ALL_UNITS, null);
        List<IVersionedId> unitIds = new ArrayList<IVersionedId>();
        for (Iterator<IInstallableUnit> unitIterator = units.iterator(); unitIterator.hasNext();) {
            IInstallableUnit unit = unitIterator.next();
            VersionedId unitId = new VersionedId(unit.getId(), unit.getVersion());
            unitIds.add(unitId);
        }
        return unitIds;
    }
}
