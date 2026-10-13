/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

public class P2ResolverAdditionalRequirementsTest {

    private static final String BUNDLE_NAMESPACE = "osgi.bundle";
    private static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;
    private static final String BUNDLE_TYPE = ArtifactType.TYPE_ECLIPSE_PLUGIN;
    private static final String IU_TYPE = ArtifactType.TYPE_INSTALLABLE_UNIT;
    private static final String TARGET_UNIT_ID = "testbundleName";

    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    private P2ResolverImpl impl;

    @BeforeEach
    public void initBlankResolver() {
        impl = new P2ResolverImpl(null, null, logVerifier.getMavenLogger(),
                Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
    }

    @Test
    public void testExactVersionMatchInTargetDefinitionUnit() throws Exception {
        String olderVersion = "2.3.3";
        String version = "2.3.4";
        String newerVersion = "2.3.5";

        String exactVersionMatchRange = "[" + version + "," + version + "]";
        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, exactVersionMatchRange);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, exactVersionMatchRange);

        List<IRequirement> requirements = impl.getAdditionalRequirements();

        IInstallableUnit matchingIU = createIU(version);
        assertIUMatchesRequirements(matchingIU, requirements);

        IInstallableUnit newerIU = createIU(newerVersion);
        assertIUDoesNotMatchRequirements(newerIU, requirements);

        IInstallableUnit olderIU = createIU(olderVersion);
        assertIUDoesNotMatchRequirements(olderIU, requirements);
    }

    private static void assertIUMatchesRequirements(IInstallableUnit unit, List<IRequirement> requirements) {
        for (IRequirement requirement : requirements) {
            Assertions.assertTrue(requirement.isMatch(unit), "IU " + unit + " must match requirement " + requirement);
        }
    }

    private static void assertIUDoesNotMatchRequirements(IInstallableUnit unit, List<IRequirement> requirements) {
        for (IRequirement requirement : requirements) {
            Assertions.assertFalse(requirement.isMatch(unit), "IU " + unit + " must not match requirement " + requirement);
        }
    }

    @Test
    public void testZeroVersionInTargetDefinitionUnit() throws Exception {
        String zeroVersion = "0.0.0";
        String arbitraryVersion = "2.5.8";

        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, zeroVersion);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, zeroVersion);

        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();

        IInstallableUnit iu = createIU(arbitraryVersion);

        Assertions.assertTrue(additionalRequirements.get(0).isMatch(iu), "Requires version 0.0.0; should be satisfied by any version");
        Assertions.assertTrue(additionalRequirements.get(1).isMatch(iu), "Requires version 0.0.0; should be satisfied by any version");
    }

    @Test
    public void testNullVersionInTargetDefinitionUnit() throws Exception {

        String nullVersion = null;
        String arbitraryVersion = "2.5.8";

        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, nullVersion);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, nullVersion);

        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();

        IInstallableUnit iu = createIU(arbitraryVersion);

        Assertions.assertTrue(additionalRequirements.get(0).isMatch(iu), "Given version was null; should be satisfied by any version");
        Assertions.assertTrue(additionalRequirements.get(1).isMatch(iu), "Given version was null; should be satisfied by any version");
    }

    @Test
    public void testAddDependencyWithVersionRange() throws Exception {
        String range = "[2.0.0,3.0.0)";
        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, range);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, range);
        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();
        String matchingVersion = "2.5.8";
        IInstallableUnit iu = createIU(matchingVersion);
        Assertions.assertTrue(additionalRequirements.get(0).isMatch(iu), "version range " + range + " should be satisfied by " + matchingVersion);
        Assertions.assertTrue(additionalRequirements.get(1).isMatch(iu), "version range " + range + " should be satisfied by " + matchingVersion);
    }

    private static IInstallableUnit createIU(String version) {
        InstallableUnitDescription iud = new InstallableUnitDescription();
        iud.setId(TARGET_UNIT_ID);
        Version osgiVersion = Version.create(version);
        iud.setVersion(osgiVersion);
        List<IProvidedCapability> list = new ArrayList<>();
        list.add(MetadataFactory.createProvidedCapability(IU_NAMESPACE, TARGET_UNIT_ID, osgiVersion));
        list.add(MetadataFactory.createProvidedCapability(BUNDLE_NAMESPACE, TARGET_UNIT_ID, osgiVersion));
        iud.addProvidedCapabilities(list);

        IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
        return iu;
    }
}
