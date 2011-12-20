package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.junit.Assert;
import org.junit.Test;

public class TargetPlatformBuilderTest extends P2ResolverTestBase {
    private static final IProgressMonitor monitor = new NullProgressMonitor();

    @Test
    public void test_addArtifactWithExistingMetadata_respects_artifact_classifiers() throws Exception {

        ArtifactMock artifact = new ArtifactMock(new File(
                "resources/pom-dependencies/org.eclipse.osgi_3.5.2.R35x_v20100126.jar"), "groupId", "artifactId", "1",
                P2Resolver.TYPE_ECLIPSE_PLUGIN, "classifier");

        ArtifactMock metadata = new ArtifactMock(new File("resources/pom-dependencies/existing-p2-metadata.xml"),
                "groupId", "artifactId", "1", P2Resolver.TYPE_ECLIPSE_PLUGIN, "p2metadata");

        P2TargetPlatform platform;
        IInstallableUnit[] units;

        // classifier does not match available metadata
        context = createP2ResolverFactory(false).createTargetPlatformBuilder(null, false);
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits().query(QueryUtil.ALL_UNITS, monitor).toArray(IInstallableUnit.class);
        Assert.assertEquals(2, units.length);
        assertContainsIU(units, "a.jre.javase");
        assertContainsIU(units, "config.a.jre.javase");

        // classifier matches one of the two IUs
        artifact.setClassifier("sources");
        context = createP2ResolverFactory(false).createTargetPlatformBuilder(null, false);
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits().query(QueryUtil.ALL_UNITS, monitor).toArray(IInstallableUnit.class);
        Assert.assertEquals(3, units.length);
        assertContainsIU(units, "a.jre.javase");
        assertContainsIU(units, "config.a.jre.javase");
        assertContainsIU(units, "test.ui.source");

        // main (i.e. null) classifier matches one of the two IUs
        artifact.setClassifier(null);
        context = createP2ResolverFactory(false).createTargetPlatformBuilder(null, false);
        context.addArtifactWithExistingMetadata(artifact, metadata);
        platform = (P2TargetPlatform) context.buildTargetPlatform();
        units = platform.getInstallableUnits().query(QueryUtil.ALL_UNITS, monitor).toArray(IInstallableUnit.class);
        Assert.assertEquals(3, units.length);
        assertContainsIU(units, "a.jre.javase");
        assertContainsIU(units, "config.a.jre.javase");
        assertContainsIU(units, "test.ui");
    }

    private void assertContainsIU(IInstallableUnit[] units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return;
            }
        }
        Assert.fail("Missing installable unit with id " + id);
    }
}
