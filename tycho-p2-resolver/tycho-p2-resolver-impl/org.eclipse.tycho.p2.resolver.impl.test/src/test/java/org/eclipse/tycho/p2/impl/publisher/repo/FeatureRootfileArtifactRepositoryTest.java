/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.repo;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class FeatureRootfileArtifactRepositoryTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRepoWithAttachedArtifacts() throws Exception {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository(createPublisherInfo(true),
                tempFolder.newFolder("testrootfiles"));

        IArtifactDescriptor artifactDescriptor = createArtifactDescriptor(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER,
                "org.eclipse.tycho.test.p2");

        subject.getOutputStream(artifactDescriptor).close();

        assertAttachedArtifact(subject.getPublishedArtifacts(), 1, "root", "org.eclipse.tycho.test.p2-1.0.0-root.zip");

        Set<IArtifactDescriptor> artifactDescriptors = subject.getArtifactDescriptors();
        Assert.assertEquals(1, artifactDescriptors.size());

        IArtifactDescriptor descriptor = artifactDescriptors.iterator().next();
        assertMavenProperties(descriptor, "root");
    }

    @Test
    public void testRepoWithAttachedArtifactsAndConfigurations() throws Exception {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository(createPublisherInfo(true),
                tempFolder.newFolder("testrootfiles"));

        IArtifactDescriptor artifactDescriptor = createArtifactDescriptor(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER,
                "org.eclipse.tycho.test.p2.win32.win32.x86");

        subject.getOutputStream(artifactDescriptor).close();

        assertAttachedArtifact(subject.getPublishedArtifacts(), 1, "root.win32.win32.x86",
                "org.eclipse.tycho.test.p2.win32.win32.x86-1.0.0-root.zip");

        Set<IArtifactDescriptor> artifactDescriptors = subject.getArtifactDescriptors();
        Assert.assertEquals(1, artifactDescriptors.size());

        IArtifactDescriptor descriptor = artifactDescriptors.iterator().next();
        assertMavenProperties(descriptor, "root.win32.win32.x86");
    }

    @Test(expected = ProvisionException.class)
    public void testRepoWithoutMavenAdvice() throws Exception {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository(createPublisherInfo(false),
                tempFolder.newFolder("testrootfiles"));

        IArtifactDescriptor artifactDescriptor = createArtifactDescriptor(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER,
                "org.eclipse.tycho.test.p2");
        subject.getOutputStream(artifactDescriptor).close();
    }

    @Test
    public void testRepoForNonBinaryArtifacts() throws Exception {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository(createPublisherInfo(true),
                tempFolder.newFolder("testrootfiles"));

        IArtifactDescriptor artifactDescriptor = createArtifactDescriptor("non-binary-classifier",
                "org.eclipse.tycho.test.p2");
        subject.getOutputStream(artifactDescriptor).close();

        Map<String, IArtifactFacade> attachedArtifacts = subject.getPublishedArtifacts();
        Assert.assertEquals(0, attachedArtifacts.size());
    }

    @Test
    public void testRepoWithInitEmptyAttachedArtifacts() {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository(null, null);
        Assert.assertEquals(0, subject.getPublishedArtifacts().size());
    }

    private void assertMavenProperties(IArtifactDescriptor descriptor, String root) {
        Assert.assertEquals(descriptor.getProperty("maven-groupId"), "artifactGroupId");
        Assert.assertEquals(descriptor.getProperty("maven-artifactId"), "artifactId");
        Assert.assertEquals(descriptor.getProperty("maven-version"), "artifactVersion");
        Assert.assertEquals(descriptor.getProperty("maven-classifier"), root);
        Assert.assertEquals(descriptor.getProperty("maven-extension"), "zip");
    }

    private void assertAttachedArtifact(Map<String, IArtifactFacade> attachedArtifacts, int expectedSize,
            String expectedClassifier, String expectedLocationFileName) {
        Assert.assertEquals(1, attachedArtifacts.size());

        IArtifactFacade artifactFacade = attachedArtifacts.get(expectedClassifier);

        Assert.assertEquals(artifactFacade.getClassidier(), expectedClassifier);
        Assert.assertEquals(artifactFacade.getLocation().getName(), expectedLocationFileName);
    }

    private PublisherInfo createPublisherInfo(boolean addMavenPropertyAdvice) {
        PublisherInfo publisherInfo = new PublisherInfo();

        publisherInfo.addAdvice(createFeatureRootAdvice());
        if (addMavenPropertyAdvice) {
            publisherInfo.addAdvice(createMavenPropertyAdvice());
        }

        return publisherInfo;
    }

    private FeatureRootAdvice createFeatureRootAdvice() {
        return new FeatureRootAdvice(rootPropertiesWithGlobalAndWindowsFiles(),
                FeatureRootAdviceTest.FEATURE_PROJECT_TEST_RESOURCE_ROOT, "artifactId");
    }

    private MavenPropertiesAdvice createMavenPropertyAdvice() {
        return new MavenPropertiesAdvice("artifactGroupId", "artifactId", "artifactVersion");
    }

    private ArtifactDescriptor createArtifactDescriptor(String classifier, String artifactId) {
        ArtifactKey key = new ArtifactKey(classifier, artifactId, Version.createOSGi(1, 0, 0));
        ArtifactDescriptor desc = new ArtifactDescriptor(key);
        return desc;
    }

    private Properties rootPropertiesWithGlobalAndWindowsFiles() {
        Properties buildProperties = new Properties();
        buildProperties.put("root.win32.win32.x86", "file:rootfiles/file1.txt");
        buildProperties.put("root", "file:rootfiles/file2.txt");
        return buildProperties;
    }

}
