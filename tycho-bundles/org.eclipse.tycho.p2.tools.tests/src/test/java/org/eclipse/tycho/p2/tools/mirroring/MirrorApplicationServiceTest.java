/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
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
package org.eclipse.tycho.p2.tools.mirroring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.publisher.DependencySeedUtil;
import org.eclipse.tycho.p2.tools.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MirrorApplicationServiceTest {

    // feature containing org.eclipse.core.runtime 3.4
    private static final String SIMPLE_FEATURE = "org.eclipse.example.original_feature";
    private static final VersionedId SIMPLE_FEATURE_IU = new VersionedId(SIMPLE_FEATURE + ".feature.group", "1.0.0");

    // patch for the SIMPLE_FEATURE replacing org.eclipse.core.runtime 3.4 by 3.5
    private static final String FEATURE_PATCH = "org.eclipse.example.feature_patch";
    private static final VersionedId FEATURE_PATCH_IU = new VersionedId(FEATURE_PATCH + ".feature.group", "1.0.0");

    private static final String DEFAULT_NAME = "dummy";
    private static final String DEFAULT_QUALIFIER = null;
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("a", "b", "c"));

    private BuildContext context;
    private DestinationRepositoryDescriptor destinationRepo;

    private MirrorApplicationServiceImpl subject;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Before
    public void initTestContext() throws Exception {
        destinationRepo = new DestinationRepositoryDescriptor(tempFolder.newFolder("dest"), DEFAULT_NAME);

        File projectFolder = tempFolder.getRoot();
        ReactorProjectIdentities currentProject = new ReactorProjectIdentitiesStub(projectFolder);
        context = new BuildContext(currentProject, DEFAULT_QUALIFIER, DEFAULT_ENVIRONMENTS);

        subject = new MirrorApplicationServiceImpl();
        MavenContext mavenContext = new MockMavenContext(null, logVerifier.getLogger());
        subject.setMavenContext(mavenContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMirrorNothing() throws Exception {
        // make sure that this unsupported case is detected; the mirror application would just mirror everything
        Collection<DependencySeed> noSeeds = Collections.emptyList();

        subject.mirrorReactor(sourceRepos("patch", "e342"), destinationRepo, noSeeds, context, false, null);
    }

    @Test
    public void testMirrorFeatureWithContent() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e342"), destinationRepo, seedFor(SIMPLE_FEATURE_IU), context, false,
                null);

        logVerifier.expectNoWarnings();
        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.4.0.v20080512.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + SIMPLE_FEATURE + "_1.0.0.jar").exists());
    }

    @Test
    public void testExtraArtifactRepositoryProperties() throws Exception {
        Map<String, String> extraArtifactRepositoryProperties = new HashMap<>(3, 1.f);
        extraArtifactRepositoryProperties.put("p2.statsURI", "http://some.where");
        extraArtifactRepositoryProperties.put("p2.mirrorsURL", "http://some.where.else");
        extraArtifactRepositoryProperties.put("foo", "bar");
        destinationRepo = new DestinationRepositoryDescriptor(tempFolder.newFolder("dest2"), DEFAULT_NAME, false, false,
                false, false, true, extraArtifactRepositoryProperties, Collections.emptyList());
        subject.mirrorReactor(sourceRepos("patch", "e342"), destinationRepo, seedFor(SIMPLE_FEATURE_IU), context, false,
                null);

        logVerifier.expectNoWarnings();
        File artifactsXml = repoFile(destinationRepo, "artifacts.xml");
        // parse manually, would be better if we can directly retrieve the repo model from the file
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(artifactsXml);
        NodeList properties = ((Element) (((Element) document.getElementsByTagName("repository").item(0))
                .getElementsByTagName("properties").item(0))).getElementsByTagName("property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            String propertyName = property.getAttribute("name");
            if (extraArtifactRepositoryProperties.containsKey(propertyName)
                    && extraArtifactRepositoryProperties.get(propertyName).equals(property.getAttribute("value"))) {
                extraArtifactRepositoryProperties.remove(propertyName);
            }
        }
        assertEquals("Artifact repository is missing extra properties", Collections.emptyMap(),
                extraArtifactRepositoryProperties);
    }

    @Test
    public void testMirrorPatch() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e352"), destinationRepo, seedFor(FEATURE_PATCH_IU), context, false,
                null);

        logVerifier.expectNoWarnings();
        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.5.0.v20090525.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + FEATURE_PATCH + "_1.0.0.jar").exists());
    }

    @Test
    public void testMirrorFeatureAndPatch() throws Exception {
        subject.mirrorReactor(sourceRepos("patch", "e352"), destinationRepo,
                seedFor(SIMPLE_FEATURE_IU, FEATURE_PATCH_IU), context, false, null);

        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.5.0.v20090525.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + SIMPLE_FEATURE + "_1.0.0.jar").exists());
        assertTrue(repoFile(destinationRepo, "features/" + FEATURE_PATCH + "_1.0.0.jar").exists());

        // logger may have warnings, which is okay because the mirror tool doesn't know the semantics of patches
    }

    @Test
    public void testMirrorWithMissingMandatoryContent() throws Exception {
        /*
         * This case is not expected to occur in integration because the p2 dependency resolver
         * ensures that all non-patched dependencies can be satisfied. If this assumption should
         * ever be violated (e.g. due to a defect elsewhere in Tycho), the build should fail. But
         * since it is not easy to distinguish between patched and unpatched dependencies, only a
         * warning is issued.
         */
        subject.mirrorReactor(sourceRepos("patch"), destinationRepo, seedFor(SIMPLE_FEATURE_IU), context, false, null);

        logVerifier.expectWarning(not(is("")));
    }

    @Test
    public void testMirrorForSeedWithNullIU() throws Exception {
        /*
         * While it is hard to get an IU from the target platform (cf. bug 412416, bug 372780), we
         * need to allow {@link DependencySeed} instances with null IU.
         */
        List<DependencySeed> seeds = Collections
                .singletonList(new DependencySeed(null, "org.eclipse.core.runtime", null));

        subject.mirrorReactor(sourceRepos("e342"), destinationRepo, seeds, context, false, null);

        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.4.0.v20080512.jar").exists());
    }

    public static RepositoryReferences sourceRepos(String... repoIds) {
        RepositoryReferences result = new RepositoryReferences();
        for (String repoId : repoIds) {
            result.addMetadataRepository(ResourceUtil.resourceFile("repositories/" + repoId));
            result.addArtifactRepository(ResourceUtil.resourceFile("repositories/" + repoId));
        }
        return result;
    }

    private static Collection<DependencySeed> seedFor(VersionedId... units) {
        Collection<DependencySeed> result = new ArrayList<>();

        for (VersionedId unit : units) {
            InstallableUnitDescription seedDescriptor = new InstallableUnitDescription();
            seedDescriptor.setId("iu-requiring." + unit.getId());
            seedDescriptor.addRequirements(strictRequirementTo(unit));
            result.add(DependencySeedUtil.createSeed(null, MetadataFactory.createInstallableUnit(seedDescriptor)));
        }
        return result;
    }

    private static Set<IRequirement> strictRequirementTo(VersionedId unit) {
        VersionRange strictRange = new VersionRange(unit.getVersion(), true, unit.getVersion(), true);
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(),
                strictRange, null, false, false);
        return Collections.singleton(requirement);
    }

    static File repoFile(DestinationRepositoryDescriptor repo, String path) {
        return new File(repo.getLocation(), path);
    }

}
