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
package org.eclipse.tycho.p2.tools.mirroring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.MavenContextImpl;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.p2.tools.test.util.ResourceUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class MirrorApplicationServiceTest {

    // feature containing org.eclipse.core.runtime 3.4
    private static final String SIMPLE_FEATURE = "org.eclipse.example.original_feature";
    private static final VersionedId SIMPLE_FEATURE_IU = new VersionedId(SIMPLE_FEATURE + ".feature.group", "1.0.0");

    // patch for the SIMPLE_FEATURE replacing org.eclipse.core.runtime 3.4 by 3.5
    private static final String FEATURE_PATCH = "org.eclipse.example.feature_patch";
    private static final VersionedId FEATURE_PATCH_IU = new VersionedId(FEATURE_PATCH + ".feature.group", "1.0.0");

    private static final String DEFAULT_NAME = "dummy";
    private static final String DEFAULT_QUALIFIER = null;
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("a", "b", "c"));

    private MemoryLog logger;
    private BuildContext context;
    private DestinationRepositoryDescriptor destinationRepo;

    private MirrorApplicationServiceImpl subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void initTestContext() {
        logger = new MemoryLog();
        destinationRepo = new DestinationRepositoryDescriptor(tempFolder.newFolder("dest"), DEFAULT_NAME);

        context = new BuildContext(DEFAULT_QUALIFIER, DEFAULT_ENVIRONMENTS, new BuildOutputDirectory(
                tempFolder.getRoot()));

        subject = new MirrorApplicationServiceImpl();
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setLogger(logger);
        subject.setMavenContext(mavenContext);
    }

    @Test
    public void testMirrorFeatureWithContent() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e342"), destinationRepo, seedFor(SIMPLE_FEATURE_IU), context, false);

        assertEquals(Collections.emptyList(), logger.warnings);
        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.4.0.v20080512.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + SIMPLE_FEATURE + "_1.0.0.jar").exists());
    }

    @Test
    public void testMirrorPatch() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e352"), destinationRepo, seedFor(FEATURE_PATCH_IU), context, false);

        assertEquals(Collections.emptyList(), logger.warnings);
        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.5.0.v20090525.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + FEATURE_PATCH + "_1.0.0.jar").exists());
    }

    @Test
    public void testMirrorFeatureAndPatch() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e352"), destinationRepo,
                seedFor(SIMPLE_FEATURE_IU, FEATURE_PATCH_IU), context, false);

        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.5.0.v20090525.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + SIMPLE_FEATURE + "_1.0.0.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + FEATURE_PATCH + "_1.0.0.jar").exists());

        // logger may have warnings, which is okay because the mirror tool doesn't know the semantics of patches
    }

    @Test
    public void testMirrorWithMissingMandatoryContent() throws Exception {
        /*
         * This case is not expected to occur in integration because the p2 dependency resolver
         * ensures that all non-patched dependencies can be satisfied. If this assumption should
         * ever be violated (e.g. due to a defect elsewhere in Tycho), the build should fail. But
         * since it is not easy to distinguish between patched and unpatched dependencies, only a
         * warning is issued.
         */
        subject.mirrorReactor(sourceRepos("patch"), destinationRepo, seedFor(SIMPLE_FEATURE_IU), context, false);

        assertTrue(logger.warnings.size() > 0);
    }

    public static RepositoryReferences sourceRepos(String... repoIds) {
        RepositoryReferences result = new RepositoryReferences();
        for (String repoId : repoIds) {
            result.addMetadataRepository(ResourceUtil.resolveTestResource("resources/repositories/" + repoId));
            result.addArtifactRepository(ResourceUtil.resolveTestResource("resources/repositories/" + repoId));
        }
        return result;
    }

    private static Collection<IInstallableUnit> seedFor(VersionedId... units) {
        ArrayList<IInstallableUnit> result = new ArrayList<IInstallableUnit>();

        for (VersionedId unit : units) {
            InstallableUnitDescription seedDescriptor = new InstallableUnitDescription();
            seedDescriptor.setId("iu-requiring." + unit.getId());
            seedDescriptor.addRequirements(strictRequirementTo(unit));
            result.add(MetadataFactory.createInstallableUnit(seedDescriptor));
        }
        return result;
    }

    private static Set<IRequirement> strictRequirementTo(VersionedId unit) {
        VersionRange strictRange = new VersionRange(unit.getVersion(), true, unit.getVersion(), true);
        IRequirement requirement = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(), strictRange,
                null, false, false);
        return Collections.singleton(requirement);
    }

    static File repoFile(DestinationRepositoryDescriptor repo, String path) {
        return new File(repo.getLocation(), path);
    }

    static class MemoryLog implements MavenLogger {
        List<String> warnings = new ArrayList<String>();

        public void error(String message) {
            throw new RuntimeException(message);
        }

        public void warn(String message) {
            warnings.add(message);
        }

        public void warn(String message, Throwable cause) {
            warnings.add(message);
        }

        public void info(String message) {
        }

        public void debug(String message) {
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isExtendedDebugEnabled() {
            return false;
        }
    }
}
