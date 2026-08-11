/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Contributors to the Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import static org.eclipse.tycho.p2tools.MirrorApplicationServiceTest.sourceRepos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TychoPlexusExtension.class)
public class MirrorStandaloneParentCategoryTest {

    @Inject
    protected PlexusContainer container;

    private static final String DEFAULT_NAME = "dummy";
    private static final String PARENT_CATEGORY_NAME = "Parent Category";
    private static final String PARENT_CATEGORY_ID = "tycho.mirroring.category.parent.category";

    private DestinationRepositoryDescriptor destinationRepo;
    private MirrorApplicationServiceImpl subject;

    @TempDir
    Path tempFolder;
    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    private BuildDirectory targetFolder;

    @BeforeEach
    public void initTestContext() throws Exception {
        IProvisioningAgent agent = container.lookup(IProvisioningAgent.class);
        agent.getService(IArtifactRepositoryManager.class);
        destinationRepo = new DestinationRepositoryDescriptor(newFolder("dest"), DEFAULT_NAME);
        subject = new MirrorApplicationServiceImpl();
        subject.setAgent(agent);
        subject.setLogger(logVerifier.getLogger());
        targetFolder = new BuildOutputDirectory(tempFolder.toFile());
    }

    @Test
    public void testParentCategoryIsInjected() throws Exception {
        MirrorOptions options = new MirrorOptions();
        options.setCategoryName(PARENT_CATEGORY_NAME);

        subject.mirrorStandalone(sourceRepos("withcategories"), destinationRepo, Collections.emptyList(), options,
                targetFolder);

        IInstallableUnit parent = findParentUnit();
        assertNotNull(parent, "Parent category IU was not injected into the destination repository");
        assertEquals(PARENT_CATEGORY_ID, parent.getId());
        assertEquals(PARENT_CATEGORY_NAME, parent.getProperty(IInstallableUnit.PROP_NAME));
        assertEquals(Boolean.TRUE.toString(), parent.getProperty(QueryUtil.PROP_TYPE_CATEGORY));
    }

    @Test
    public void testParentWrapsAllSourceCategories() throws Exception {
        MirrorOptions options = new MirrorOptions();
        options.setCategoryName(PARENT_CATEGORY_NAME);

        subject.mirrorStandalone(sourceRepos("withcategories"), destinationRepo, Collections.emptyList(), options,
                targetFolder);

        IInstallableUnit parent = findParentUnit();
        assertNotNull(parent);

        Set<String> requiredIds = parent.getRequirements().stream()
                .map(r -> r.getMatches().getParameters()[0].toString()).collect(Collectors.toSet());

        assertTrue(requiredIds.contains("org.eclipse.example.category.alpha"), "Parent should require test.category.alpha");
        assertTrue(requiredIds.contains("org.eclipse.example.category.beta"), "Parent should require test.category.beta");
        assertEquals(2, requiredIds.size(), "Parent should require exactly the two source categories");
    }

    @Test
    public void testNoCategoryNameProducesNoParentUnit() throws Exception {
        subject.mirrorStandalone(sourceRepos("withcategories"), destinationRepo, Collections.emptyList(),
                new MirrorOptions(), targetFolder);

        IInstallableUnit parent = findParentUnit();
        assertTrue(parent == null, "No parent IU should be present when categoryName is not set");
    }

    @Test
    public void testParentNotDuplicatedOnAppend() throws Exception {
        MirrorOptions options = new MirrorOptions();
        options.setCategoryName(PARENT_CATEGORY_NAME);

        // Mirror twice to the same destination (append=true is the default)
        subject.mirrorStandalone(sourceRepos("withcategories"), destinationRepo, Collections.emptyList(), options,
                targetFolder);
        subject.mirrorStandalone(sourceRepos("withcategories"), destinationRepo, Collections.emptyList(), options,
                targetFolder);

        IQueryResult<IInstallableUnit> result = openDestinationRepo().query(QueryUtil.createIUQuery(PARENT_CATEGORY_ID),
                null);
        long count = StreamSupport.stream(result.spliterator(), false).count();
        assertEquals(1, count, "Parent category IU must appear exactly once after two mirror runs");
    }

    // --- helpers ---

    private IInstallableUnit findParentUnit() throws Exception {
        IQueryResult<IInstallableUnit> result = openDestinationRepo().query(QueryUtil.createIUQuery(PARENT_CATEGORY_ID),
                null);
        return result.isEmpty() ? null : result.iterator().next();
    }

    private IMetadataRepository openDestinationRepo() throws Exception {
        IProvisioningAgent agent = container.lookup(IProvisioningAgent.class);
        IMetadataRepositoryManager mgr = agent.getService(IMetadataRepositoryManager.class);
        return mgr.loadRepository(destinationRepo.getLocation().toURI(), null);
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempFolder.resolve(path)).toFile();
    }
}
