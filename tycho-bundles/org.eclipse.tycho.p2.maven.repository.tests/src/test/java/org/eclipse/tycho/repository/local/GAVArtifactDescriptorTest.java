/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.junit.Test;

@SuppressWarnings("restriction")
public class GAVArtifactDescriptorTest {

    private static final IArtifactKey TEST_KEY = new ArtifactKey("p2.class", "p2.id", Version.create("4.3.0.20130614"));
    private static final GAV TEST_GAV = new GAV("mvn.group", "mvn.id", "4.3.0-SNAPSHOT");

    private static final String DEFAULT_CLASSIFIER = null;
    private static final String OTHER_CLASSIFIER = "mvn.classifier";

    private static final String DEFAULT_EXTENSION = "jar";
    private static final String OTHER_EXTENSION = "mvn.fileextension";

    private GAVArtifactDescriptor subject;

    @Test
    public void testCreation() {
        MavenRepositoryCoordinates coordinates = new MavenRepositoryCoordinates(TEST_GAV, OTHER_CLASSIFIER,
                OTHER_EXTENSION);
        subject = new GAVArtifactDescriptor(createP2Descriptor(), coordinates);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(), is(coordinates));
    }

    @Test
    public void testCreationFromP2Key() {
        subject = new GAVArtifactDescriptor(TEST_KEY);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(), is(new MavenRepositoryCoordinates("p2.p2.class", "p2.id",
                "4.3.0.20130614", DEFAULT_CLASSIFIER, DEFAULT_EXTENSION)));
    }

    @Test
    public void testCreationFromPlainP2Descriptor() {
        ArtifactDescriptor input = createP2Descriptor();
        // no maven properties set
        subject = new GAVArtifactDescriptor(input);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(), is(new MavenRepositoryCoordinates("p2.p2.class", "p2.id",
                "4.3.0.20130614", DEFAULT_CLASSIFIER, DEFAULT_EXTENSION)));
    }

    @Test
    public void testDeserialization() {
        // parsing to p2's implementation of IArtifactDescriptor is done elsewhere, so assume this is the input
        ArtifactDescriptor input = createP2Descriptor();
        input.setProperty("maven-groupId", TEST_GAV.getGroupId());
        input.setProperty("maven-artifactId", TEST_GAV.getArtifactId());
        input.setProperty("maven-version", TEST_GAV.getVersion());

        subject = new GAVArtifactDescriptor(input);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(),
                is(new MavenRepositoryCoordinates(TEST_GAV, DEFAULT_CLASSIFIER, DEFAULT_EXTENSION)));
    }

    @Test
    public void testDeserializationWithClassifierAndExt() {
        ArtifactDescriptor input = createP2Descriptor();
        setGAVProperties(input);
        input.setProperty("maven-classifier", OTHER_CLASSIFIER);
        input.setProperty("maven-extension", OTHER_EXTENSION);

        subject = new GAVArtifactDescriptor(input);

        assertThat(subject.getMavenCoordinates(),
                is(new MavenRepositoryCoordinates(TEST_GAV, OTHER_CLASSIFIER, OTHER_EXTENSION)));
    }

    @Test
    public void testDeserializationWithRedundantDefaultExt() {
        ArtifactDescriptor input = createP2Descriptor();
        setGAVProperties(input);
        input.setProperty("maven-extension", DEFAULT_EXTENSION);

        subject = new GAVArtifactDescriptor(input);

        assertThat(subject.getMavenCoordinates(),
                is(new MavenRepositoryCoordinates(TEST_GAV, DEFAULT_CLASSIFIER, DEFAULT_EXTENSION)));
    }

    @Test
    public void testDeserializationWithPartialGAV() {
        ArtifactDescriptor input = createP2Descriptor();
        input.setProperty("maven-groupId", TEST_GAV.getGroupId());
        input.setProperty("maven-artifactId", TEST_GAV.getArtifactId());

        subject = new GAVArtifactDescriptor(input);

        // treated like completely missing properties
        assertThat(subject.getMavenCoordinates(), is(new MavenRepositoryCoordinates("p2.p2.class", "p2.id",
                "4.3.0.20130614", DEFAULT_CLASSIFIER, DEFAULT_EXTENSION)));
    }

    @Test
    public void testSerializationRoundTrip() {
        MavenRepositoryCoordinates coordinates = new MavenRepositoryCoordinates(TEST_GAV, OTHER_CLASSIFIER,
                OTHER_EXTENSION);
        GAVArtifactDescriptor original = new GAVArtifactDescriptor(createP2Descriptor(), coordinates);

        subject = serializeAndDeSerialize(original);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(), is(coordinates));
    }

    @Test
    public void testSerializationOmitsDefaults() {
        MavenRepositoryCoordinates coordinates = new MavenRepositoryCoordinates(TEST_GAV, DEFAULT_CLASSIFIER,
                DEFAULT_CLASSIFIER);
        subject = new GAVArtifactDescriptor(createP2Descriptor(), coordinates);

        ArtifactDescriptor serialized = new ArtifactDescriptor(subject);

        assertThat(serialized.getProperties().keySet(), not(hasItem("maven-classifier")));
        assertThat(serialized.getProperties().keySet(), not(hasItem("maven-extension")));
    }

    @Test
    public void testExplicitCoordinatesOverwritesProperties() {
        ArtifactDescriptor input = createP2Descriptor();
        setGAVProperties(input);
        input.setProperty("maven-classifier", OTHER_CLASSIFIER);
        input.setProperty("maven-extension", OTHER_EXTENSION);

        MavenRepositoryCoordinates explicitCoordinatesWithDefaults = new MavenRepositoryCoordinates(TEST_GAV,
                DEFAULT_CLASSIFIER, DEFAULT_EXTENSION);
        GAVArtifactDescriptor original = new GAVArtifactDescriptor(input, explicitCoordinatesWithDefaults);

        subject = serializeAndDeSerialize(original);

        assertThat(subject.getArtifactKey(), is(TEST_KEY));
        assertThat(subject.getMavenCoordinates(), is(explicitCoordinatesWithDefaults));
    }

    @Test
    public void testGetLocalRepositoryPath() {
        MavenRepositoryCoordinates coordinates = new MavenRepositoryCoordinates(TEST_GAV, OTHER_CLASSIFIER,
                OTHER_EXTENSION);
        subject = new GAVArtifactDescriptor(createP2Descriptor(), coordinates);

        assertThat(subject.getMavenCoordinates().getLocalRepositoryPath(new MavenContextImpl(null, false, null, null) {

            @Override
            public String getExtension(String artifactType) {
                return artifactType;
            }
        }), is("mvn/group/mvn.id/4.3.0-SNAPSHOT/mvn.id-4.3.0-SNAPSHOT-mvn.classifier.mvn.fileextension"));
    }

    @Test
    public void testGetLocalRepositoryPathWithDefaults() {
        MavenRepositoryCoordinates coordinates = new MavenRepositoryCoordinates(TEST_GAV, DEFAULT_CLASSIFIER,
                DEFAULT_EXTENSION);
        subject = new GAVArtifactDescriptor(createP2Descriptor(), coordinates);

        assertThat(subject.getMavenCoordinates().getLocalRepositoryPath(new MavenContextImpl(null, false, null, null) {

            @Override
            public String getExtension(String artifactType) {
                return "jar";
            }
        }), is("mvn/group/mvn.id/4.3.0-SNAPSHOT/mvn.id-4.3.0-SNAPSHOT.jar"));
    }

    private static ArtifactDescriptor createP2Descriptor() {
        return new ArtifactDescriptor(TEST_KEY);
    }

    private static void setGAVProperties(ArtifactDescriptor input) {
        input.setProperty("maven-groupId", TEST_GAV.getGroupId());
        input.setProperty("maven-artifactId", TEST_GAV.getArtifactId());
        input.setProperty("maven-version", TEST_GAV.getVersion());
    }

    private static GAVArtifactDescriptor serializeAndDeSerialize(GAVArtifactDescriptor original) {
        ArtifactDescriptor serialized = new ArtifactDescriptor(original);
        GAVArtifactDescriptor deserialized = new GAVArtifactDescriptor(serialized);
        return deserialized;
    }

}
