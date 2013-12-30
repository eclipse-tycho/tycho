/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.MAIN_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V2;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unit;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TargetPlatformFactoryTest {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private TargetPlatformConfigurationStub tpConfig;

    private TargetPlatformFactoryImpl subject;

    @Before
    public void setUpSubjectAndContext() throws Exception {
        subject = new TestResolverFactory(logVerifier.getLogger()).getTargetPlatformFactoryImpl();

        tpConfig = new TargetPlatformConfigurationStub();
        tpConfig.setEnvironments(Collections.singletonList(new TargetEnvironment(null, null, null))); // dummy value for target file resolution
    }

    @Test
    public void test364134_publishFinalMetadata() throws Exception {
        String groupId = "org.eclipse.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        ReactorProjectStub project = new ReactorProjectStub(new File(
                "resources/platformbuilder/publish-complete-metadata/bundle"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN);

        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();

        DependencyMetadata metadata = impl.generateMetadata(new ArtifactMock(new File(
                "resources/platformbuilder/publish-complete-metadata/bundle"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN), environments);

        project.setDependencyMetadata(metadata);

        List<ReactorProject> reactorProjects = Collections.<ReactorProject> singletonList(project);
        P2TargetPlatform platform = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, reactorProjects,
                null);

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertEquals("1.0.0.qualifier", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion().toString());

        // publish "complete" metedata
        metadata = impl.generateMetadata(new ArtifactMock(new File(
                "resources/platformbuilder/publish-complete-metadata/bundle-complete"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null), environments);
        project.setDependencyMetadata(metadata);

        units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertEquals("1.0.0.123abc", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion().toString());
    }

    @Test
    public void test364134_classifiedAttachedArtifactMetadata() throws Exception {
        ReactorProject reactorProject = createReactorProject(new File(
                "resources/platformbuilder/classified-attached-artifacts"), new String[] {
                "org.eclipse.tycho.p2.impl.test.bundle", "org.eclipse.tycho.p2.impl.test.bundle.source" },
                new String[] { "org.eclipse.tycho.p2.impl.test.bundle.secondary" });

        P2TargetPlatform platform = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                Collections.singletonList(reactorProject), null);

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(3, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.source");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.secondary");

        Collection<IInstallableUnit> projectPrimaryIUs = platform.getReactorProjectIUs(reactorProject.getBasedir(),
                true);

        assertEquals(2, projectPrimaryIUs.size());
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.source");

        Collection<IInstallableUnit> projectSecondaryIUs = platform.getReactorProjectIUs(reactorProject.getBasedir(),
                false);
        assertEquals(1, projectSecondaryIUs.size());
        assertContainsIU(projectSecondaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.secondary");
    }

    @Test
    public void testReactorProjectFiltering() throws Exception {
        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "iu.p2.inf"));
        tpConfig.addFilters(Arrays.asList(filter));

        File projectRoot = new File("dummy");
        ReactorProject reactorProject = createReactorProject(projectRoot,
                "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group", "iu.p2.inf");
        P2TargetPlatform platform = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                Collections.singletonList(reactorProject), null);

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
        // assertContainsIU(units, "iu.p2.inf"); removed by the filter

        units = platform.getReactorProjectIUs(projectRoot, true);
        assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
    }

    @Test
    public void testTargetFileCannotContributeOtherVersionsOfUnitsProducedByReactor() {
        tpConfig.addTargetDefinition(targetDefinition(TestRepositories.V1_AND_V2, MAIN_BUNDLE));

        // reactor artifact produces a unit with same ID
        // TODO make produced version more explicit
        File projectRoot = new File("dummy");
        ReactorProject reactorProject = createReactorProject(projectRoot, MAIN_BUNDLE.getId(), null);
        P2TargetPlatform platform = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                Collections.singletonList(reactorProject), null);

        assertThat(platform.getInstallableUnits(), hasItem(unit(MAIN_BUNDLE.getId(), "1.0.2"))); // from reactor
        assertThat(platform.getInstallableUnits(),
                not(hasItem(unit(MAIN_BUNDLE.getId(), MAIN_BUNDLE.getVersion().toString())))); // from target file
    }

    @Test
    public void testIncludeLocalMavenRepo() throws Exception {
        TestResolverFactory factory = new TestResolverFactory(logVerifier.getLogger());
        LocalMetadataRepository localMetadataRepo = factory.getLocalMetadataRepository();
        // add one IU to local repo
        localMetadataRepo.addInstallableUnit(InstallableUnitUtil.createIU("locallyInstalledIU", "1.0.0"), new GAV(
                "test", "foo", "1.0.0"));
        subject = factory.getTargetPlatformFactoryImpl();
        Collection<IInstallableUnit> iusIncludingLocalRepo = subject.createTargetPlatform(tpConfig,
                NOOP_EE_RESOLUTION_HANDLER, null, null).getInstallableUnits();
        tpConfig.setForceIgnoreLocalArtifacts(true);
        Collection<IInstallableUnit> iusWithoutLocalRepo = subject.createTargetPlatform(tpConfig,
                NOOP_EE_RESOLUTION_HANDLER, null, null).getInstallableUnits();
        Set<IInstallableUnit> retainedIUs = new HashSet<IInstallableUnit>(iusIncludingLocalRepo);
        retainedIUs.removeAll(iusWithoutLocalRepo);
        assertEquals(1, retainedIUs.size());
        assertEquals("locallyInstalledIU", retainedIUs.iterator().next().getId());
    }

    @Test
    public void testMultipleIndependentlyResolvedTargetFiles() throws Exception {
        tpConfig.addTargetDefinition(plannerTargetDefinition(TestRepositories.V1, REFERENCED_BUNDLE_V1));
        tpConfig.addTargetDefinition(plannerTargetDefinition(TestRepositories.V2, REFERENCED_BUNDLE_V2));
        P2TargetPlatform tp = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null, null);
        // platforms must have been resolved in two planner calls because otherwise the singleton bundles would have collided

        assertThat(versionedIdsOf(tp), hasItem(REFERENCED_BUNDLE_V1));
        assertThat(versionedIdsOf(tp), hasItem(REFERENCED_BUNDLE_V2));
    }

    private static TargetDefinition plannerTargetDefinition(TestRepositories repository, IVersionedId unit) {
        TargetDefinition.Location location = new TargetDefinitionResolverIncludeModeTests.PlannerLocationStub(
                repository, unit);
        return new TargetDefinitionResolverTest.TargetDefinitionStub(Collections.singletonList(location));
    }

    private static TargetDefinition targetDefinition(TestRepositories repository, IVersionedId unit) {
        TargetDefinition.Location location = new TargetDefinitionResolverTest.LocationStub(repository, unit);
        return new TargetDefinitionResolverTest.TargetDefinitionStub(Collections.singletonList(location));
    }

    private ReactorProject createReactorProject(File projectRoot, String primaryUnitId, String secondaryUnitId) {
        return createReactorProject(projectRoot, asArrayUnlessNull(primaryUnitId), asArrayUnlessNull(secondaryUnitId));
    }

    private String[] asArrayUnlessNull(String string) {
        return string == null ? null : new String[] { string };
    }

    private ReactorProject createReactorProject(File projectRoot, String[] primaryUnitIds, String[] secondaryUnitIds) {
        // TODO ReactorProjectMock constructor with less nulls?
        ReactorProjectStub result = new ReactorProjectStub(projectRoot, null, null, null, null);

        DependencyMetadata dependencyMetadata = new DependencyMetadata();
        dependencyMetadata.setMetadata(true, createUnits(primaryUnitIds));
        dependencyMetadata.setMetadata(false, createUnits(secondaryUnitIds));
        result.setDependencyMetadata(dependencyMetadata);

        return result;
    }

    private static List<IInstallableUnit> createUnits(String[] unitIds) {
        if (unitIds == null) {
            return Collections.emptyList();
        } else {
            List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
            for (String unitId : unitIds) {
                result.add(InstallableUnitUtil.createIU(unitId, "1.0.2"));
            }
            return result;
        }
    }

    private void assertContainsIU(Collection<IInstallableUnit> units, String id) {
        assertThat(units, hasItem(unitWithId(id)));
    }

    private IInstallableUnit getIU(Collection<IInstallableUnit> units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return unit;
            }
        }
        fail("Missing installable unit with id " + id);
        return null;
    }

    static Collection<IVersionedId> versionedIdsOf(P2TargetPlatform platform) {
        Collection<IVersionedId> result = new ArrayList<IVersionedId>();
        for (IInstallableUnit unit : platform.getInstallableUnits()) {
            result.add(new VersionedId(unit.getId(), unit.getVersion()));
        }
        return result;
    }
}
