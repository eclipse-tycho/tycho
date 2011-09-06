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
package org.eclipse.tycho.p2.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.impl.repo.MetadataSerializableImpl;
import org.eclipse.tycho.test.util.InstallableUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetadataSerializableImplTest {

    private IProvisioningAgent agent;

    @Before
    public void setUp() throws ProvisionException {
        agent = Activator.newProvisioningAgent();
    }

    @After
    public void tearDown() {
        agent.stop();
    }

    @Test
    public void testSerializeAndLoadWithEmptyIUList() throws IOException, ProvisionException,
            OperationCanceledException {

        File tmpDir = createTempDir("repo");
        try {
            Set<IInstallableUnit> units = new HashSet<IInstallableUnit>();
            MetadataSerializableImpl subject = new MetadataSerializableImpl();
            serialize(subject, units, tmpDir);
            Assert.assertEquals(units, deserialize(tmpDir));
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    @Test
    public void testSerializeAndLoad() throws IOException, ProvisionException, OperationCanceledException {

        File tmpDir = createTempDir("repo");
        try {
            Set<IInstallableUnit> units = new HashSet<IInstallableUnit>(Arrays.asList(InstallableUnitUtil.createIU(
                    "org.example.test", "1.0.0")));
            MetadataSerializableImpl subject = new MetadataSerializableImpl();
            serialize(subject, units, tmpDir);
            Assert.assertEquals(units, deserialize(tmpDir));
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    private Set<IInstallableUnit> deserialize(File tmpDir) throws ProvisionException {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(tmpDir.toURI(), null);
        IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.ALL_UNITS, null);
        Set<IInstallableUnit> result = queryResult.toSet();
        return result;
    }

    private void serialize(MetadataSerializableImpl subject, Set<?> units, File tmpDir) throws FileNotFoundException,
            IOException {
        FileOutputStream os = new FileOutputStream(new File(tmpDir, "content.xml"));
        try {
            subject.serialize(os, units);
        } finally {
            os.close();
        }
    }

    private void deleteRecursive(File tmpDir) {
        for (File file : tmpDir.listFiles()) {
            if (file.isDirectory()) {
                deleteRecursive(file);
            }
            file.delete();
        }
    }

    private File createTempDir(String prefix) throws IOException {
        File directory = File.createTempFile(prefix, "");
        if (directory.delete()) {
            directory.mkdirs();
            return directory;
        } else {
            throw new IOException("Could not create temp directory at: " + directory.getAbsolutePath());
        }
    }
}
