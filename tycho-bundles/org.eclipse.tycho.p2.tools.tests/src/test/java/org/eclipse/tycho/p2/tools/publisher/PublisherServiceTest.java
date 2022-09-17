/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP SE and others.
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
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.test.util.ResourceUtil.resourceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.testutil.InstallableUnitUtil;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.p2base.metadata.ImmutableInMemoryMetadataRepository;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class PublisherServiceTest {

    private static final String DEFAULT_QUALIFIER = "1.2.3.testqual";
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("testos", "testws", "testarch"));

    @Rule
    public LogVerifier logVerifier = new LogVerifier();
    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();

    private PublishingRepository outputRepository;
    private PublisherService subject;

    @Before
    public void initSubject() throws Exception {
        File projectDirectory = tempManager.newFolder("projectDir");

        LinkedHashSet<IInstallableUnit> installableUnits = new LinkedHashSet<>();
        installableUnits.add(InstallableUnitUtil.createFeatureIU("org.eclipse.example.original_feature", "1.0.0"));
        IMetadataRepository context = new ImmutableInMemoryMetadataRepository(installableUnits);

        // TODO these publishers don't produce artifacts, so we could run without file system
        outputRepository = new PublishingRepositoryImpl(p2Context.getAgent(),
                new ReactorProjectIdentitiesStub(projectDirectory));
        PublisherActionRunner publisherRunner = new PublisherActionRunner(context, DEFAULT_ENVIRONMENTS,
                logVerifier.getLogger());
        subject = new PublisherServiceImpl(publisherRunner, DEFAULT_QUALIFIER, outputRepository);
    }

    @Test
    public void testCategoryPublishing() throws Exception {
        File categoryDefinition = resourceFile("publishers/category.xml");

        Collection<DependencySeed> seeds = subject.publishCategories(categoryDefinition);

        assertEquals(1, seeds.size());
        DependencySeed seed = seeds.iterator().next();

        Set<IInstallableUnit> publishedUnits = outputRepository.getInstallableUnits();
        assertTrue(publishedUnits.contains(seed.getInstallableUnit()));
    }

    @Test
    public void testProfilePublishing() throws Exception {
        File customProfile = resourceFile("publishers/virgo-1.6.profile");
        Collection<DependencySeed> seeds = subject.publishEEProfile(customProfile);
        assertEquals(2, seeds.size());
        IInstallableUnit virgoProfileIU = unitsById(seeds).get("a.jre.virgo");
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
        assertTrue(unitsById(seeds).keySet().contains("config.a.jre.virgo"));
    }

    @Test(expected = FacadeException.class)
    public void testValidateProfileFile() throws Exception {
        ((PublisherServiceImpl) subject).validateProfile(resourceFile("publishers/inconsistentname-1.0.profile"));
    }

    /**
     * Returns the installable units from the given dependency seeds, indexed by the installable
     * units's IDs.
     */
    private static Map<String, IInstallableUnit> unitsById(Collection<DependencySeed> seeds) {
        Map<String, IInstallableUnit> result = new HashMap<>();
        for (DependencySeed seed : seeds) {
            IInstallableUnit iu = seed.getInstallableUnit();
            result.put(iu.getId(), iu);
        }
        return result;
    }

}
