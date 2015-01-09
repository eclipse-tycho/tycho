/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.test.util.ResourceUtil.resourceFile;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.p2base.metadata.ImmutableInMemoryMetadataRepository;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class PublishProductToolTest {

    private static final String DEFAULT_FLAVOR = "tooling";
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("testos", "testws", "testarch"));

    @Rule
    public LogVerifier logVerifier = new LogVerifier();
    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();

    @Rule
    public ExpectedException thrownException = ExpectedException.none();

    private PublishingRepository outputRepository;
    private PublishProductTool subject;

    @Before
    public void initSubject() throws Exception {
        File projectDirectory = tempManager.newFolder("projectDir");

        LinkedHashSet<IInstallableUnit> installableUnits = new LinkedHashSet<IInstallableUnit>();
        installableUnits.add(InstallableUnitUtil.createFeatureIU("org.eclipse.example.original_feature", "1.0.0"));
        IMetadataRepository context = new ImmutableInMemoryMetadataRepository(installableUnits);

        outputRepository = new PublishingRepositoryImpl(p2Context.getAgent(), new ReactorProjectIdentitiesStub(
                projectDirectory));
        PublisherActionRunner publisherRunner = new PublisherActionRunner(context, DEFAULT_ENVIRONMENTS,
                logVerifier.getLogger());
        subject = new PublishProductToolImpl(publisherRunner, outputRepository);
    }

    @Test
    public void testProductPublishing() throws Exception {
        File productDefinition = resourceFile("publishers/test.product");
        File launcherBinaries = resourceFile("launchers/");

        Collection<DependencySeed> seeds = subject.publishProduct(productDefinition, launcherBinaries, DEFAULT_FLAVOR);

        assertThat(seeds.size(), is(1));
        DependencySeed seed = seeds.iterator().next();

        Set<Object> publishedUnits = outputRepository.getInstallableUnits();
        assertThat(publishedUnits, hasItem(seed.getInstallableUnit()));

        // test for launcher artifact
        Map<String, File> artifactLocations = outputRepository.getArtifactLocations();
        // TODO 348586 drop productUid from classifier
        String executableClassifier = "productUid.executable.testws.testos.testarch";
        assertThat(artifactLocations.keySet(), hasItem(executableClassifier));
        assertThat(artifactLocations.get(executableClassifier), isFile());
        assertThat(artifactLocations.get(executableClassifier).toString(), endsWith(".zip"));
    }

    @Test
    public void testProductPublishingWithMissingFragments() throws Exception {
        // product referencing a fragment that is not in the target platform -> publisher must fail because the dependency resolution no longer detects this (see bug 342890)
        File productDefinition = resourceFile("publishers/missingFragment.product");
        File launcherBinaries = resourceFile("launchers/");

        thrownException.expectMessage(containsString("org.eclipse.core.filesystem.hpux.ppc"));
        subject.publishProduct(productDefinition, launcherBinaries, DEFAULT_FLAVOR);
    }

}
