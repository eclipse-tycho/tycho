/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.test.util.ArtifactMock;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

public class P2MetadataGeneratorImplTest {

    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    @Test
    public void gav() throws Exception {
        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setMavenContext(new MockMavenContext(null, logVerifier.getLogger()));
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        File location = new File("src/test/resources/generator/bundle").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        List<TargetEnvironment> environments = new ArrayList<>();
        DependencyMetadata metadata = impl.generateMetadata(
                new ArtifactMock(location, groupId, artifactId, version, PackagingType.TYPE_ECLIPSE_PLUGIN),
                environments, new PublisherOptions());

        List<IInstallableUnit> units = new ArrayList<>(metadata.getInstallableUnits());
        List<IArtifactDescriptor> artifacts = new ArrayList<>(metadata.getArtifactDescriptors());

        Assertions.assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        Assertions.assertEquals("org.eclipse.tycho.p2.impl.test.bundle", unit.getId());
        Assertions.assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        Assertions.assertEquals(4, unit.getRequirements().size());

        Assertions.assertEquals(1, artifacts.size());
        IArtifactDescriptor ad = artifacts.iterator().next();
        Assertions.assertEquals("org.eclipse.tycho.p2.impl.test.bundle", ad.getArtifactKey().getId());
        Assertions.assertEquals("1.0.0.qualifier", ad.getArtifactKey().getVersion().toString());

        Assertions.assertEquals(groupId, ad.getProperties().get(TychoConstants.PROP_GROUP_ID));
        Assertions.assertEquals(artifactId, ad.getProperties().get(TychoConstants.PROP_ARTIFACT_ID));
        Assertions.assertEquals(version, ad.getProperties().get(TychoConstants.PROP_VERSION));
    }

    @Test
    public void testDownloadStats() throws Exception {
        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setMavenContext(new MockMavenContext(null, logVerifier.getLogger()));
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        File location = new File("src/test/resources/generator/bundle").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        List<TargetEnvironment> environments = new ArrayList<>();

        DependencyMetadata metadata = impl.generateMetadata(
                new ArtifactMock(location, groupId, artifactId, version, PackagingType.TYPE_ECLIPSE_PLUGIN),
                environments, new PublisherOptions());
        assertNull(metadata.getArtifactDescriptors().iterator().next().getProperty("download.stats"));

        PublisherOptions options = new PublisherOptions();
        options.setGenerateDownloadStats(true);
        metadata = impl.generateMetadata(
                new ArtifactMock(location, groupId, artifactId, version, PackagingType.TYPE_ECLIPSE_PLUGIN),
                environments, options);
        assertEquals("org.eclipse.tycho.p2.impl.test.bundle/1.0.0.qualifier",
                metadata.getArtifactDescriptors().iterator().next().getProperty("download.stats"));
    }

    @Test
    public void generateFeatureMetadata() throws Exception {
        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setMavenContext(new MockMavenContext(null, logVerifier.getLogger()));
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        File location = new File("src/test/resources/generator/feature").getCanonicalFile();

        DependencyMetadata metadata = impl.generateMetadata(new ArtifactMock(location,
                "org.eclipse.tycho.p2.impl.test", "feature", "1.0.0-SNAPSHOT", PackagingType.TYPE_ECLIPSE_FEATURE),
                List.of(), new PublisherOptions());

        assertTrue(metadata.getInstallableUnits().stream()
                .anyMatch(unit -> "org.eclipse.tycho.p2.impl.test.feature.feature.group".equals(unit.getId())));
    }

    @Test
    public void reportFeatureLocationWhenMetadataCannotBeRead() throws Exception {
        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setMavenContext(new MockMavenContext(null, logVerifier.getLogger()));
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        File location = new File("src/test/resources/generator/bundle").getCanonicalFile();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> impl.generateMetadata(new ArtifactMock(location, "org.eclipse.tycho.p2.impl.test", "feature",
                        "1.0.0-SNAPSHOT", PackagingType.TYPE_ECLIPSE_FEATURE), List.of(), new PublisherOptions()));

        assertTrue(exception.getMessage().contains("Unable to read feature metadata"));
        assertTrue(exception.getMessage().contains(location.getAbsolutePath()));
        assertFalse(exception.getMessage().contains("Feature.setLocation"));
    }
}
