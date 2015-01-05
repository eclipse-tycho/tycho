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

import static org.eclipse.tycho.p2.tools.test.util.ResourceUtil.resolveTestResource;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.p2base.metadata.ImmutableInMemoryMetadataRepository;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.eclipse.tycho.test.util.StubServiceRegistration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class PublisherServiceTest {

    private static final String DEFAULT_QUALIFIER = "1.2.3.testqual";
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
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<MavenContext>(
            MavenContext.class, createMavenContext(logVerifier.getLogger()));

    @Rule
    public ExpectedException thrownException = ExpectedException.none();

    private PublisherService subject;

    private PublishingRepository outputRepository;

    @Before
    public void initSubject() throws Exception {
        File projectDirectory = tempManager.newFolder("projectDir");

        LinkedHashSet<IInstallableUnit> installableUnits = new LinkedHashSet<IInstallableUnit>();
        installableUnits.add(InstallableUnitUtil.createFeatureIU("org.eclipse.example.original_feature", "1.0.0"));
        IMetadataRepository context = new ImmutableInMemoryMetadataRepository(installableUnits);

        outputRepository = new PublishingRepositoryImpl(p2Context.getAgent(), new ReactorProjectIdentitiesStub(
                projectDirectory));
        PublisherInfoTemplate publisherConfiguration = new PublisherInfoTemplate(context, DEFAULT_ENVIRONMENTS);
        subject = new PublisherServiceImpl(publisherConfiguration, DEFAULT_QUALIFIER, outputRepository,
                logVerifier.getLogger());
    }

    @Test
    public void testCategoryPublishing() throws Exception {
        File categoryDefinition = resolveTestResource("resources/publishers/category.xml");

        Collection<DependencySeed> seeds = subject.publishCategories(categoryDefinition);

        assertThat(seeds.size(), is(1));
        DependencySeed seed = seeds.iterator().next();

        Set<Object> publishedUnits = outputRepository.getInstallableUnits();
        assertThat(publishedUnits, hasItem(seed.getInstallableUnit()));
    }

    @Test
    public void testProfilePublishing() throws Exception {
        File customProfile = resolveTestResource("resources/publishers/virgo-1.6.profile");
        Collection<DependencySeed> seeds = subject.publishEEProfile(customProfile);
        assertEquals(2, seeds.size());
        Map<String, IInstallableUnit> resultMap = new HashMap<String, IInstallableUnit>();
        for (DependencySeed seed : seeds) {
            IInstallableUnit iu = (IInstallableUnit) seed.getInstallableUnit();
            resultMap.put(iu.getId(), iu);
        }
        IInstallableUnit virgoProfileIU = resultMap.get("a.jre.virgo");
        assertNotNull(virgoProfileIU);
        Collection<IProvidedCapability> provided = virgoProfileIU.getProvidedCapabilities();
        boolean customJavaxActivationVersionFound = false;
        Version version_1_1_1 = Version.create("1.1.1");
        for (IProvidedCapability capability : provided) {
            if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(capability.getNamespace())) {
                if ("javax.activation".equals(capability.getName())) {
                    if (version_1_1_1.equals(capability.getVersion())) {
                        customJavaxActivationVersionFound = true;
                        break;
                    }
                }
            }
        }
        assertTrue("did not find capability for package javax.activation with custom version " + version_1_1_1,
                customJavaxActivationVersionFound);
        IInstallableUnit configVirgoProfileIU = resultMap.get("config.a.jre.virgo");
        assertNotNull(configVirgoProfileIU);
    }

    @Test(expected = FacadeException.class)
    public void testValidateProfileFile() throws Exception {
        ((PublisherServiceImpl) subject)
                .validateProfile(resolveTestResource("resources/publishers/inconsistentname-1.0.profile"));
    }

    @Test
    public void testProductPublishing() throws Exception {
        File productDefinition = resolveTestResource("resources/publishers/test.product");
        File launcherBinaries = resolveTestResource("resources/launchers/");

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
        File productDefinition = resolveTestResource("resources/publishers/missingFragment.product");
        File launcherBinaries = resolveTestResource("resources/launchers/");

        thrownException.expectMessage(containsString("org.eclipse.core.filesystem.hpux.ppc"));
        subject.publishProduct(productDefinition, launcherBinaries, DEFAULT_FLAVOR);
    }

    private static MavenContext createMavenContext(MavenLogger mavenLogger) {
        MavenContext mavenContext = new MavenContextImpl(null, mavenLogger);
        return mavenContext;
    }

}
