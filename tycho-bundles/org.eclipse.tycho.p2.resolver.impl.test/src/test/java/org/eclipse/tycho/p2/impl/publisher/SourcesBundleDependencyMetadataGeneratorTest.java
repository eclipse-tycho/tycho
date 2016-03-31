package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SourcesBundleDependencyMetadataGeneratorTest {

    @Test
    public void testToCanonicalVersion() {
        String releaseVersion = SourcesBundleDependencyMetadataGenerator.toCanonicalVersion("1.0.0");
        String snapshotVersion = SourcesBundleDependencyMetadataGenerator.toCanonicalVersion("1.0.0-SNAPSHOT");
        String buildnumberVersion = SourcesBundleDependencyMetadataGenerator.toCanonicalVersion("1.0.0-123");
        String buildnumberAndMilestoneVersion = SourcesBundleDependencyMetadataGenerator
                .toCanonicalVersion("1.0.0-123-99");

        assertEquals(releaseVersion, "1.0.0");
        assertEquals(snapshotVersion, "1.0.0.qualifier");
        assertEquals(buildnumberVersion, "1.0.0.123");
        assertEquals(buildnumberAndMilestoneVersion, "1.0.0.123-99");
    }

}
