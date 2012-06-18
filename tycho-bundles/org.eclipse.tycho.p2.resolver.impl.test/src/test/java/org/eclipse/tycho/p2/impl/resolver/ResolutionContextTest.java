/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split external target platform and "resolution context" with reactor artifacts
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import static org.eclipse.tycho.p2.impl.resolver.TargetPlatformBuilderTest.assertContainsIU;
import static org.eclipse.tycho.p2.impl.resolver.TargetPlatformBuilderTest.getIU;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResolutionContextTest extends P2ResolverTestBase {

    private ResolutionContext subject;

    @Before
    public void initSubject() throws Exception {
        subject = new ResolutionContext(new MavenLoggerStub());
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
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        DependencyMetadata metadata = impl.generateMetadata(artifact, environments);

        artifact.setDependencyMetadata(metadata);

        subject.addReactorArtifact(artifact);

        Collection<IInstallableUnit> units = subject.getInstallableUnits();
        Assert.assertEquals(1, units.size());
        Assert.assertEquals("1.0.0.qualifier", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion()
                .toString());

        // publish "complete" metedata 
        metadata = impl.generateMetadata(new ArtifactMock(new File(
                "resources/platformbuilder/publish-complete-metadata/bundle-complete"), groupId, artifactId, version,
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null), environments);
        artifact.setDependencyMetadata(metadata);

        units = subject.getInstallableUnits();
        Assert.assertEquals(1, units.size());
        Assert.assertEquals("1.0.0.123abc", getIU(units, "org.eclipse.tycho.p2.impl.test.bundle").getVersion()
                .toString());

    }

    @Test
    public void test364134_classifiedAttachedArtifactMetadata() throws Exception {
        ArtifactMock artifact = new ArtifactMock(new File("resources/platformbuilder/classified-attached-artifacts"),
                "org.eclipse.tycho.p2.impl.test.bundle", "org.eclipse.tycho.p2.impl.test.bundle", "1.0.0-SNAPSHOT",
                ArtifactKey.TYPE_ECLIPSE_PLUGIN, null);
        P2GeneratorImpl generatorImpl = new P2GeneratorImpl(false);
        generatorImpl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        List<Map<String, String>> environments = new ArrayList<Map<String, String>>();
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

        subject.addReactorArtifact(artifact);
        subject.addReactorArtifact(secondaryArtifact);
        subject.addReactorArtifact(sourceArtifact);

        Collection<IInstallableUnit> units = subject.getInstallableUnits();
        Assert.assertEquals(3, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.source");
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.bundle.secondary");

        Collection<IInstallableUnit> projectPrimaryIUs = subject.getReactorProjectIUs(artifact.getLocation(), true);

        Assert.assertEquals(2, projectPrimaryIUs.size());
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle");
        assertContainsIU(projectPrimaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.source");

        Collection<IInstallableUnit> projectSecondaryIUs = subject.getReactorProjectIUs(artifact.getLocation(), false);
        Assert.assertEquals(1, projectSecondaryIUs.size());
        assertContainsIU(projectSecondaryIUs, "org.eclipse.tycho.p2.impl.test.bundle.secondary");
    }

    @Test
    public void testReactorProjectFiltering() throws Exception {

        TargetPlatformFilter filter = TargetPlatformFilter.removeAllFilter(CapabilityPattern.patternWithoutVersion(
                CapabilityType.P2_INSTALLABLE_UNIT, "iu.p2.inf"));
        subject.addFilters(Arrays.asList(filter));

        File projectRoot = new File("resources/platformbuilder/feature-p2-inf").getCanonicalFile();
        addReactorProject(projectRoot, ArtifactKey.TYPE_ECLIPSE_FEATURE, "org.eclipse.tycho.p2.impl.test.bundle-p2-inf");

        Collection<IInstallableUnit> units = subject.getInstallableUnits();
        Assert.assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
        // assertContainsIU(units, "iu.p2.inf"); removed by the filter

        units = subject.getReactorProjectIUs(projectRoot, true);
        Assert.assertEquals(units.toString(), 1, units.size());
        assertContainsIU(units, "org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group");
    }

    @Override
    void addReactorProject(File projectRoot, String packagingType, String artifactId) {
        ArtifactMock artifact = new ArtifactMock(projectRoot, DEFAULT_GROUP_ID, artifactId, DEFAULT_VERSION,
                packagingType);
        IDependencyMetadata metadata = dependencyGenerator.generateMetadata(artifact, getEnvironments(),
                OptionalResolutionAction.REQUIRE);
        artifact.setDependencyMetadata(metadata);
        subject.addReactorArtifact(artifact);
    }
}
