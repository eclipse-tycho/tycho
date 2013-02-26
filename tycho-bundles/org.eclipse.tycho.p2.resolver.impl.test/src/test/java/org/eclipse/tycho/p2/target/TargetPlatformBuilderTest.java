/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;

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
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.resolver.P2ResolverTestBase;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.InstallableUnitUtil;
import org.junit.Test;

public class TargetPlatformBuilderTest extends P2ResolverTestBase {

    @Test
    public void test_addArtifactWithExistingMetadata_respects_artifact_classifiers() throws Exception {

        ArtifactMock artifact = new ArtifactMock(new File(
                "resources/platformbuilder/pom-dependencies/org.eclipse.osgi_3.5.2.R35x_v20100126.jar"), "groupId",
                "artifactId", "1", ArtifactKey.TYPE_ECLIPSE_PLUGIN, "classifier");

        ArtifactMock metadata = new ArtifactMock(new File(
                "resources/platformbuilder/pom-dependencies/existing-p2-metadata.xml"), "groupId", "artifactId", "1",
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, "p2metadata");

        P2TargetPlatform platform;
        Collection<IInstallableUnit> units;

        // classifier does not match available metadata
        context = createTargetPlatformBuilder();
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits();
        assertEquals(0, units.size());

        // classifier matches one of the two IUs
        artifact.setClassifier("sources");
        context = createTargetPlatformBuilder();
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertContainsIU(units, "test.ui.source");

        // main (i.e. null) classifier matches one of the two IUs
        artifact.setClassifier(null);
        context = createTargetPlatformBuilder();
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertContainsIU(units, "test.ui");
    }

    @Test
    public void test364134_publishFinalMetadata() throws Exception {
        String groupId = "org.eclipse.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        ArtifactMock artifact = new ArtifactMock(
                new File("resources/platformbuilder/publish-complete-metadata/bundle"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null);

        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();

        DependencyMetadata metadata = impl.generateMetadata(artifact, environments);

        artifact.setDependencyMetadata(metadata);

        context = createTargetPlatformBuilder();
        context.addReactorArtifact(artifact);

        P2TargetPlatform platform = context.buildTargetPlatform();

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertEquals("1.0.0.qualifier", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion().toString());

        // publish "complete" metedata
        metadata = impl.generateMetadata(new ArtifactMock(new File(
                "resources/platformbuilder/publish-complete-metadata/bundle-complete"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null), environments);
        artifact.setDependencyMetadata(metadata);

        units = platform.getInstallableUnits();
        assertEquals(1, units.size());
        assertEquals("1.0.0.123abc", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion().toString());

    }

    @Test
    public void test364134_classifiedAttachedArtifactMetadata() throws Exception {
        ArtifactMock artifact = new ArtifactMock(new File("resources/platformbuilder/classified-attached-artifacts"),
                "org.eclipse.tycho.p2.impl.test.bundle", "org.eclipse.tycho.p2.impl.test.bundle", "1.0.0-SNAPSHOT",
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null);
        P2GeneratorImpl generatorImpl = new P2GeneratorImpl(false);
        generatorImpl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();
        DependencyMetadata metadata = generatorImpl.generateMetadata(artifact, environments);
        artifact.setDependencyMetadata(metadata);

        ArtifactMock secondaryArtifact = new ArtifactMock(artifact.getLocation(), artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getVersion(), ArtifactKey.TYPE_ECLIPSE_PLUGIN, "secondary");
        DependencyMetadata secondaryMetadata = new DependencyMetadata();
        secondaryMetadata.setMetadata(true, Collections.<IInstallableUnit> emptyList());
        secondaryMetadata.setMetadata(
                false,
                generatorImpl.generateMetadata(
                        new ArtifactMock(new File(artifact.getLocation(), "secondary"), artifact.getGroupId(), artifact
                                .getArtifactId(), artifact.getVersion(), ArtifactKey.TYPE_ECLIPSE_PLUGIN, "secondary"),
                        environments).getInstallableUnits());
        secondaryArtifact.setDependencyMetadata(secondaryMetadata);

        ArtifactMock sourceArtifact = new ArtifactMock(artifact.getLocation(), artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getVersion(), ArtifactKey.TYPE_ECLIPSE_PLUGIN, "sources");
        DependencyMetadataGenerator sourcesGeneratorImpl = new SourcesBundleDependencyMetadataGenerator();
        IDependencyMetadata sourcesMetadata = sourcesGeneratorImpl.generateMetadata(sourceArtifact, environments, null);
        sourceArtifact.setDependencyMetadata(sourcesMetadata);

        context = createTargetPlatformBuilder();
        context.addReactorArtifact(artifact);
        context.addReactorArtifact(secondaryArtifact);
        context.addReactorArtifact(sourceArtifact);

        P2TargetPlatform platform = context.buildTargetPlatform();

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(3, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.source");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.secondary");

        Collection<IInstallableUnit> projectPrimaryIUs = platform.getReactorProjectIUs(artifact.getLocation(), true);

        assertEquals(2, projectPrimaryIUs.size());
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.source");

        Collection<IInstallableUnit> projectSecondaryIUs = platform.getReactorProjectIUs(artifact.getLocation(), false);
        assertEquals(1, projectSecondaryIUs.size());
        assertContainsIU(projectSecondaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.secondary");
    }

    @Test
    public void testReactorProjectFiltering() throws Exception {
        context = createTargetPlatformBuilder();

        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "iu.p2.inf"));
        context.addFilters(Arrays.asList(filter));

        File projectRoot = ResourceUtil.resourceFile("platformbuilder/feature-p2-inf");
        addReactorProject(projectRoot, ArtifactKey.TYPE_ECLIPSE_FEATURE, "org.eclipse.tycho.p2.impl.test.bundle-p2-inf");

        P2TargetPlatform platform = context.buildTargetPlatform();

        Collection<IInstallableUnit> units = platform.getInstallableUnits();
        assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
        // assertContainsIU(units, "iu.p2.inf"); removed by the filter

        units = platform.getReactorProjectIUs(projectRoot, true);
        assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
    }

    @Test
    public void testIncludeLocalMavenRepo() throws Exception {
        TestTargetPlatformBuilderFactory factory = new TestTargetPlatformBuilderFactory(logVerifier.getLogger());
        LocalMetadataRepository localMetadataRepo = factory.getLocalMetadataRepository();
        // add one IU to local repo
        localMetadataRepo.addInstallableUnit(InstallableUnitUtil.createIU("locallyInstalledIU", "1.0.0"), new GAV(
                "test", "foo", "1.0.0"));
        TargetPlatformBuilderImpl tpBuilder = factory.createTargetPlatformBuilder();
        Collection<IInstallableUnit> iusIncludingLocalRepo = tpBuilder.buildTargetPlatform().getInstallableUnits();
        tpBuilder.setIncludeLocalMavenRepo(false);
        Collection<IInstallableUnit> iusWithoutLocalRepo = tpBuilder.buildTargetPlatform().getInstallableUnits();
        Set<IInstallableUnit> retainedIUs = new HashSet<IInstallableUnit>(iusIncludingLocalRepo);
        retainedIUs.removeAll(iusWithoutLocalRepo);
        assertEquals(1, retainedIUs.size());
        assertEquals("locallyInstalledIU", retainedIUs.iterator().next().getId());
    }

    @Test
    public void testAddMultipleIndependentlyResolvedTargetFiles() throws Exception {
        List<TargetEnvironment> env = Collections.singletonList(new TargetEnvironment(null, null, null));

        TargetPlatformBuilderImpl tpBuilder = createTargetPlatformBuilder();
        tpBuilder.addTargetDefinition(plannerTargetDefinition(TestRepositories.V1, REFERENCED_BUNDLE_V1), env);
        tpBuilder.addTargetDefinition(plannerTargetDefinition(TestRepositories.V2, REFERENCED_BUNDLE_V2), env);
        P2TargetPlatform tp = tpBuilder.buildTargetPlatform();
        // platforms must have been resolved in two planner calls because otherwise the singleton bundles would have collided

        assertThat(versionedIdsOf(tp), hasItem(REFERENCED_BUNDLE_V1));
        assertThat(versionedIdsOf(tp), hasItem(REFERENCED_BUNDLE_V2));
    }

    private static TargetDefinition plannerTargetDefinition(TestRepositories repository, IVersionedId unit) {
        TargetDefinition.Location location = new TargetDefinitionResolverIncludeModeTests.PlannerLocationStub(
                repository, unit);
        return new TargetDefinitionResolverTest.TargetDefinitionStub(Collections.singletonList(location));
    }

    private void assertContainsIU(Collection<IInstallableUnit> units, String id) {
        assertNotNull("Missing installable unit with id " + id, getIU(units, id));
    }

    protected IInstallableUnit getIU(Collection<IInstallableUnit> units, String id) {
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
