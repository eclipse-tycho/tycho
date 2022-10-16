/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.eclipse.tycho.p2.facade.MetadataSerializableImpl;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetadataSerializableImplTest extends TychoPlexusTestCase {

    private IProvisioningAgent agent;

    @BeforeEach
    protected void setUp() throws Exception {
        agent = lookup(IProvisioningAgent.class);
    }

    @Test
    public void testSerializeAndLoadWithEmptyIUList()
            throws IOException, ProvisionException, OperationCanceledException {

        File tmpDir = createTempDir("repo");
        try {
            Set<IInstallableUnit> units = new HashSet<>();
            MetadataSerializableImpl subject = new MetadataSerializableImpl();
            serialize(subject, units, tmpDir);
            assertEquals(units, deserialize(tmpDir));
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    @Test
    public void testSerializeAndLoad() throws IOException, ProvisionException, OperationCanceledException {

        File tmpDir = createTempDir("repo");
        try {
            Set<IInstallableUnit> units = new HashSet<>(
                    Arrays.asList(InstallableUnitUtil.createIU("org.example.test", "1.0.0")));
            MetadataSerializableImpl subject = new MetadataSerializableImpl();
            serialize(subject, units, tmpDir);
            assertEquals(units, deserialize(tmpDir));
        } finally {
            deleteRecursive(tmpDir);
        }
    }

    private Set<IInstallableUnit> deserialize(File tmpDir) throws ProvisionException {
        IMetadataRepositoryManager manager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepository repository = manager.loadRepository(tmpDir.toURI(), null);
        IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.ALL_UNITS, null);
        Set<IInstallableUnit> result = queryResult.toSet();
        return result;
    }

    private void serialize(MetadataSerializableImpl subject, Set<IInstallableUnit> units, File tmpDir)
            throws FileNotFoundException, IOException {
        try (FileOutputStream os = new FileOutputStream(new File(tmpDir, "content.xml"))) {
            subject.serialize(os, units);
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
