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
package org.eclipse.tycho.p2.maven.repository.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.p2.impl.repo.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.junit.Test;

public class LocalMetadataRepositoryTest extends BaseMavenRepositoryTest {
    private IProgressMonitor monitor = new NullProgressMonitor();

    @Test
    public void emptyRepository() throws CoreException {
        File location = new File("target/empty");
        createRepository(location, "group", "artifact", "version");

        IMetadataRepository repository = loadRepository(location);
        Assert.assertNotNull(repository);
    }

    protected IMetadataRepository loadRepository(File location) throws ProvisionException {
        return new LocalMetadataRepository(location.toURI(), createMetadataIndex(location), new LocalRepositoryReader(
                location));
    }

    protected LocalMetadataRepository createRepository(File location, String groupId, String artifactId, String version)
            throws ProvisionException {
        location.mkdirs();
        File metadataFile = new File(location, FileBasedTychoRepositoryIndex.METADATA_INDEX_RELPATH);
        metadataFile.delete();
        metadataFile.getParentFile().mkdirs();
        TychoRepositoryIndex metadataIndex = createMetadataIndex(location);
        return new LocalMetadataRepository(location.toURI(), metadataIndex);
    }

    @Test
    public void addInstallableUnit() throws CoreException {
        File location = new File("target/metadataRepo");
        LocalMetadataRepository repository = createRepository(location, "group", "artifact", "version");

        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId("test");
        iud.setVersion(Version.parseVersion("1.0.0"));

        iud.setProperty(RepositoryLayoutHelper.PROP_GROUP_ID, "group");
        iud.setProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID, "artifact");
        iud.setProperty(RepositoryLayoutHelper.PROP_VERSION, "version");

        InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
        iud2.setId("test2");
        iud2.setVersion(Version.parseVersion("1.0.0"));

        iud2.setProperty(RepositoryLayoutHelper.PROP_GROUP_ID, "group");
        iud2.setProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID, "artifact2");
        iud2.setProperty(RepositoryLayoutHelper.PROP_VERSION, "version");

        IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
        IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);
        repository.addInstallableUnits(Arrays.asList(iu, iu2));

        repository = (LocalMetadataRepository) loadRepository(location);

        IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.ALL_UNITS, monitor);
        ArrayList<IInstallableUnit> allius = new ArrayList<IInstallableUnit>(result.toSet());
        Assert.assertEquals(2, allius.size());

        // as of e3.5.2 Collector uses HashSet internally and does not guarantee collected results order
        // 3.6 IQueryResult, too, is backed by HashSet. makes no sense.
        // Assert.assertEquals( iu.getId(), allius.get( 0 ).getId() );

        Set<IInstallableUnit> ius = repository.getGAVs().get(RepositoryLayoutHelper.getGAV(iu.getProperties()));
        Assert.assertEquals(1, ius.size());
    }

    @Test
    public void testOutdatedIndex() throws CoreException {
        // create and fill repo
        File location = new File("target/indexmetadataRepo");
        LocalMetadataRepository repository = createRepository(location, "group", "artifact", "version");
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId("test");
        iud.setVersion(Version.parseVersion("1.0.0"));
        iud.setProperty(RepositoryLayoutHelper.PROP_GROUP_ID, "group");
        iud.setProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID, "artifact");
        iud.setProperty(RepositoryLayoutHelper.PROP_VERSION, "version");
        IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
        repository.addInstallableUnits(Arrays.asList(iu));
        repository = (LocalMetadataRepository) loadRepository(location);

        // check: the artifact is in the index
        TychoRepositoryIndex metaIndex = createMetadataIndex(location);
        Assert.assertFalse(metaIndex.getProjectGAVs().isEmpty());

        // delete artifact from file system
        deleteDir(new File(location, "group"));

        // create a new repo and check that the reference was gracefully removed from the index
        repository = (LocalMetadataRepository) loadRepository(location);
        repository.save();
        metaIndex = createMetadataIndex(location);
        Assert.assertTrue(metaIndex.getProjectGAVs().isEmpty());

    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                }
                file.delete();
            }
        }
    }

}
