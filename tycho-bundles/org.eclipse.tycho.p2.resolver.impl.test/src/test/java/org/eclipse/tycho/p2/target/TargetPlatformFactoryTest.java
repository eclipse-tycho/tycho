/*******************************************************************************
 * Copyright (c) 2008, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - added tests; re-write of all previously existing tests
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.MAIN_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V2;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unit;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithIdAndVersion;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TargetPlatformFactoryTest {

    private static final ReactorProjectIdentities DUMMY_PROJECT = new ReactorProjectIdentitiesStub(new File("dummy"));

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
    public void testFinalTargetPlatformNotContainsPreliminaryReactorIU() throws Exception {
        List<ReactorProject> preliminaryReactor = Arrays.asList(createReactorProject(new File("dummy"), "reactor.id",
                null));
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                preliminaryReactor, null);

        // final TP without any reactor content
        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);

        assertThat(finalTP.getInstallableUnits(), not(hasItem(unitWithId("reactor.id"))));
    }

    @Test
    public void testFinalTargetPlatformContainsExternalRepoIU() throws Exception {
        tpConfig.addP2Repository(ResourceUtil.resourceFile("repositories/launchers").toURI());
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null, null);

        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);

        assertThat(finalTP.getInstallableUnits(), hasItem(unitWithId("org.eclipse.equinox.launcher")));
    }

    @Test
    public void testFinalTargetPlatformContainsTargetFileIU() {
        tpConfig.addTargetDefinition(targetDefinition(TestRepositories.V1_AND_V2, MAIN_BUNDLE));
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null, null);

        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);

        assertThat(finalTP.getInstallableUnits(),
                hasItem(unit(MAIN_BUNDLE.getId(), MAIN_BUNDLE.getVersion().toString())));
    }

    @Test
    public void testFinalTargetPlatformContainsPomDependencyIU() throws Exception {
        PomDependencyCollector pomDependencies = subject.newPomDependencyCollector();
        pomDependencies.addArtifactWithExistingMetadata(PomDependencyCollectorTest.artifactWithClassifier(null),
                PomDependencyCollectorTest.existingMetadata());
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null,
                pomDependencies);

        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);

        assertThat(finalTP.getInstallableUnits(), hasItem(unitWithId("test.unit")));
    }

    @Test
    public void testFinalTargetPlatformContainsExecutionEnvironmentIU() throws Exception {
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig,
                ExecutionEnvironmentTestUtils.standardEEResolutionHintProvider("J2SE-1.4"), null, null);

        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);

        assertThat(finalTP.getInstallableUnits(), hasItem(unit("a.jre.j2se", "1.4.0")));
    }

    @Test
    public void testFinalTargetPlatformContainsFinalReactorIU() throws Exception {
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null, null);

        Map<IInstallableUnit, ReactorProjectIdentities> finalUnits = Collections.singletonMap(
                InstallableUnitUtil.createIU("bundle", "1.2.0"), DUMMY_PROJECT);
        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, finalUnits);

        assertThat(finalTP.getInstallableUnits(), hasItem(unit("bundle", "1.2.0")));
    }

    // TODO 372035 test logging for potential bugs in the explicit filters configuration

    @Test
    public void testConfiguredFiltersOnReactorIUsInPreliminaryTP() throws Exception {
        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "iu.p2.inf"));
        tpConfig.addFilters(Arrays.asList(filter));

        ReactorProject reactorProject = createReactorProject(new File("dummy"), "test.feature.feature.group",
                "iu.p2.inf");
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                Collections.singletonList(reactorProject), null);

        assertThat(preliminaryTP.getInstallableUnits(), hasItem(unitWithId("test.feature.feature.group")));
        assertThat(preliminaryTP.getInstallableUnits(), not(hasItem(unitWithId("iu.p2.inf"))));
    }

    @Test
    public void testConfiguredFiltersOnReactorIUsInFinalTP() throws Exception {
        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "iu.p2.inf"));
        tpConfig.addFilters(Arrays.asList(filter));
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null, null);

        Map<IInstallableUnit, ReactorProjectIdentities> finalUnits = new HashMap<IInstallableUnit, ReactorProjectIdentities>();
        finalUnits.put(InstallableUnitUtil.createIU("test.feature.feature.group"), DUMMY_PROJECT);
        finalUnits.put(InstallableUnitUtil.createIU("iu.p2.inf"), DUMMY_PROJECT);
        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, finalUnits);

        assertThat(finalTP.getInstallableUnits(), hasItem(unitWithId("test.feature.feature.group")));
        assertThat(finalTP.getInstallableUnits(), not(hasItem(unitWithId("iu.p2.inf"))));
    }

    @Test
    public void testConfiguredFiltersOnPomDependencies() throws Exception {
        PomDependencyCollector pomDependencies = subject.newPomDependencyCollector();
        pomDependencies.addArtifactWithExistingMetadata(PomDependencyCollectorTest.artifactWithClassifier(null),
                PomDependencyCollectorTest.existingMetadata());

        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "test.unit"));
        tpConfig.addFilters(Arrays.asList(filter));

        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, null,
                pomDependencies);
        assertThat(preliminaryTP.getInstallableUnits(), not(hasItem(unitWithId("test.unit"))));

        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, null);
        assertThat(finalTP.getInstallableUnits(), not(hasItem(unitWithId("test.unit"))));
    }

    @Test
    public void testOtherVersionsOfReactorIUsAreFilteredFromExternalContent() throws Exception {
        // contains trt.bundle/1.0.0.201108051343
        tpConfig.addP2Repository(ResourceUtil.resourceFile("targetresolver/v1_content").toURI());

        // reactor artifact produces a unit with same ID
        ReactorProject reactorProject = createReactorProject(new File("dummy"), "trt.bundle/1.5.5.qualifier", null);
        P2TargetPlatform preliminaryTP = subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER,
                Collections.singletonList(reactorProject), null);

        assertThat(preliminaryTP.getInstallableUnits(), hasItem(unit("trt.bundle", "1.5.5.qualifier")));
        assertThat(preliminaryTP.getInstallableUnits(), not(hasItem(unit("trt.bundle", "1.0.0.201108051343"))));

        Map<IInstallableUnit, ReactorProjectIdentities> finalUnits = Collections.singletonMap(
                InstallableUnitUtil.createIU("trt.bundle", "1.5.5.20140216"), reactorProject.getIdentities());
        P2TargetPlatform finalTP = subject.createTargetPlatformWithUpdatedReactorUnits(preliminaryTP, finalUnits);

        assertThat(finalTP.getInstallableUnits(), hasItem(unit("trt.bundle", "1.5.5.20140216")));
        assertThat(finalTP.getInstallableUnits(), not(hasItem(unit("trt.bundle", "1.0.0.201108051343"))));
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

        assertThat(tp.getInstallableUnits(), hasItem(unitWithIdAndVersion(REFERENCED_BUNDLE_V1)));
        assertThat(tp.getInstallableUnits(), hasItem(unitWithIdAndVersion(REFERENCED_BUNDLE_V2)));
    }

    @Test(expected = DuplicateReactorIUsException.class)
    public void testDuplicateReactorUnits() throws Exception {
        List<ReactorProject> reactorProjects = new ArrayList<ReactorProject>();
        reactorProjects.add(createReactorProject(new File("location1"), "unit.a", "unit.b"));
        reactorProjects.add(createReactorProject(new File("location2"), "unit.b", null));
        subject.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, reactorProjects, null);
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
        ReactorProjectStub result = new ReactorProjectStub(projectRoot);

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
                result.add(InstallableUnitUtil.createIU(unitId));
            }
            return result;
        }
    }

}
