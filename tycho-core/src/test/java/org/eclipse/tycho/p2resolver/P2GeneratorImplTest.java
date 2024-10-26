/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.test.util.InstallableUnitMatchers.hasGAV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.test.util.ArtifactMock;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.junit.Rule;
import org.junit.Test;

public class P2GeneratorImplTest {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Test
    public void testGenerateSourceBundleMetadata() throws Exception {
        SourcesBundleDependencyMetadataGenerator p2GeneratorImpl = new SourcesBundleDependencyMetadataGenerator(new MockMavenContext(null, logVerifier.getLogger()), null);
        File location = new File("src/test/resources/generator/bundle").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "org.acme", "foo", "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = p2GeneratorImpl
                .generateMetadata(artifactMock, getEnvironments(), null, new PublisherOptions())
                .getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit sourceBundleUnit = getUnit("foo.source", units);
        assertNotNull(sourceBundleUnit);
        assertEquals(Version.create("0.0.1"), sourceBundleUnit.getVersion());
        assertThat(sourceBundleUnit, hasGAV("org.acme", "foo", "0.0.1", "sources"));
        ITouchpointData touchPointData = sourceBundleUnit.getTouchpointData().iterator().next();
        String manifestContent = touchPointData.getInstruction("manifest").getBody();
        Manifest manifest = new Manifest(new ByteArrayInputStream(manifestContent.getBytes(StandardCharsets.UTF_8)));
        Attributes attributes = manifest.getMainAttributes();
        assertEquals("foo.source", attributes.getValue("Bundle-SymbolicName"));
        //assertEquals("foo;version=0.0.1;roots:=\".\"", attributes.getValue("Eclipse-SourceBundle"));
    }

    @Test
    public void generateSourceBundleMetadataForProjectWithP2Inf() throws Exception {
        // p2.inf must not leak into sources bundle

        SourcesBundleDependencyMetadataGenerator p2GeneratorImpl = new SourcesBundleDependencyMetadataGenerator(new MockMavenContext(null, logVerifier.getLogger()), null);
        File location = new File("src/test/resources/generator/bundle-p2-inf").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "org.acme", "foo", "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = p2GeneratorImpl
                .generateMetadata(artifactMock, getEnvironments(), null, new PublisherOptions())
                .getDependencyMetadata();

        assertEquals(1, units.size());

        IInstallableUnit unit = getUnit("foo.source", units);
        assertEquals(0, unit.getRequirements().size());
    }

    private IInstallableUnit getUnit(String id, Set<IInstallableUnit> units) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return unit;
            }
        }
        return null;
    }

    private List<TargetEnvironment> getEnvironments() {
        return Collections.singletonList(new TargetEnvironment("linux", "gtk", "x86_64"));
    }

    @Test
    public void testOptionalImportPackage_REQUIRE() throws Exception {
        DefaultDependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-import-package").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-import-package", "optional-import-package",
                "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.REQUIRE, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-import-package", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        IRequiredCapability requirement = getReqCap(requirements, PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE,
                "org.osgi.framework");
        assertNotNull("org.osgi.framework", requirement);
        assertTrue(requirement.isGreedy());
        assertEquals(1, requirement.getMin());
        assertEquals(1, requirement.getMax());
        assertEquals(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, requirement.getNamespace());
        assertEquals("org.osgi.framework", requirement.getName());
    }

    private IRequiredCapability getReqCap(List<IRequirement> requirements, String ns, String name) {
        for (IRequirement r : requirements) {
            if (r instanceof IRequiredCapability cap) {
                if (ns.equals(cap.getNamespace()) && name.equals(cap.getName())) {
                    return cap;
                }
            }
        }
        return null;
    }

    private DefaultDependencyMetadataGenerator createDependencyMetadataGenerator() {
        return new DefaultDependencyMetadataGenerator(new MockMavenContext(null, logVerifier.getLogger()), new BuildPropertiesParserForTesting(), null);
    }

    @Test
    public void testOptionalImportPackage_IGNORE() throws Exception {
        DependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-import-package").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-import-package", "optional-import-package",
                "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.IGNORE, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-import-package", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        assertEquals(1, requirements.size());
    }

    @Test
    public void testOptionalRequireBundle_REQUIRE() throws Exception {
        DependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-require-bundle").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-require-bundle", "optional-require-bundle",
                "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.REQUIRE, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-require-bundle", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        IRequiredCapability requirement = getReqCap(requirements, BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
                "org.eclipse.osgi");
        assertNotNull(requirement);
        assertTrue(requirement.isGreedy());
        assertEquals(1, requirement.getMin());
        assertEquals(1, requirement.getMax());
    }

    @Test
    public void testOptionalRequireBundle_OPTIONAL() throws Exception {
        DependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-require-bundle").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-require-bundle", "optional-require-bundle",
                "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.OPTIONAL, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-require-bundle", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        IRequiredCapability requirement = getReqCap(requirements, BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
                "org.eclipse.osgi");
        assertFalse(requirement.isGreedy());
        assertEquals(0, requirement.getMin());
        assertEquals(1, requirement.getMax());
    }

    @Test
    public void testOptionalRequireBundle_IGNORE() throws Exception {
        DependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-require-bundle").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-require-bundle", "optional-require-bundle",
                "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.IGNORE, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-require-bundle", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        assertEquals(1, requirements.size());
    }

    @Test
    public void testOptionalRequireBundleP2inf_REQUIRE() throws Exception {
        DependencyMetadataGenerator generator = createDependencyMetadataGenerator();
        File location = new File("src/test/resources/generator/optional-reqiure-bundle-p2inf").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "optional-reqiure-bundle-p2inf",
                "optional-reqiure-bundle-p2inf", "0.0.1", "eclipse-plugin");
        Set<IInstallableUnit> units = generator.generateMetadata(artifactMock, getEnvironments(),
                OptionalResolutionAction.REQUIRE, new PublisherOptions()).getDependencyMetadata();
        assertEquals(1, units.size());
        IInstallableUnit iu = getUnit("optional-reqiure-bundle-p2inf", units);
        assertNotNull(iu);
        List<IRequirement> requirements = new ArrayList<>(iu.getRequirements());
        IRequiredCapability requirement = getReqCap(requirements, BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
                "org.eclipse.osgi");
        assertTrue(requirement.isGreedy());
        assertEquals(1, requirement.getMin());
        assertEquals(1, requirement.getMax());
    }
}
