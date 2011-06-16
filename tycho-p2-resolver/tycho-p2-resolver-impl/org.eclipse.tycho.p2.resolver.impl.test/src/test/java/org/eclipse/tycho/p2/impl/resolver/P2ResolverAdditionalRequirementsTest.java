/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.impl.resolver.P2ResolverImpl;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverAdditionalRequirementsTest {

    private static final String BUNDLE_NAMESPACE = "osgi.bundle";

    private static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;

    private static final String BUNDLE_TYPE = "eclipse-plugin";

    private static final String IU_TYPE = P2Resolver.TYPE_INSTALLABLE_UNIT;

    private static final String TARGET_UNIT_ID = "testbundleName";

    private P2ResolverImpl impl;

    @Before
    public void initBlankResolver() {
        impl = new P2ResolverImpl(new MavenLoggerStub());
    }

    @Test
    public void testExactVersionMatchInTargetDefinitionUnit() {
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
            Assert.assertTrue("IU " + unit + " must match requirement " + requirement, requirement.isMatch(unit));
        }
    }

    private static void assertIUDoesNotMatchRequirements(IInstallableUnit unit, List<IRequirement> requirements) {
        for (IRequirement requirement : requirements) {
            Assert.assertFalse("IU " + unit + " must not match requirement " + requirement, requirement.isMatch(unit));
        }
    }

    @Test
    public void testZeroVersionInTargetDefinitionUnit() {
        String zeroVersion = "0.0.0";
        String arbitraryVersion = "2.5.8";

        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, zeroVersion);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, zeroVersion);

        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();

        IInstallableUnit iu = createIU(arbitraryVersion);

        Assert.assertTrue("Requires version 0.0.0; should be satisfied by any version", additionalRequirements.get(0)
                .isMatch(iu));
        Assert.assertTrue("Requires version 0.0.0; should be satisfied by any version", additionalRequirements.get(1)
                .isMatch(iu));
    }

    @Test
    public void testNullVersionInTargetDefinitionUnit() {

        String nullVersion = null;
        String arbitraryVersion = "2.5.8";

        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, nullVersion);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, nullVersion);

        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();

        IInstallableUnit iu = createIU(arbitraryVersion);

        Assert.assertTrue("Given version was null; should be satisfied by any version", additionalRequirements.get(0)
                .isMatch(iu));
        Assert.assertTrue("Given version was null; should be satisfied by any version", additionalRequirements.get(1)
                .isMatch(iu));
    }

    @Test
    public void testAddDependencyWithVersionRange() {
        String range = "[2.0.0,3.0.0)";
        impl.addDependency(IU_TYPE, TARGET_UNIT_ID, range);
        impl.addDependency(BUNDLE_TYPE, TARGET_UNIT_ID, range);
        List<IRequirement> additionalRequirements = impl.getAdditionalRequirements();
        String matchingVersion = "2.5.8";
        IInstallableUnit iu = createIU(matchingVersion);
        Assert.assertTrue("version range " + range + " should be satisfied by " + matchingVersion,
                additionalRequirements.get(0).isMatch(iu));
        Assert.assertTrue("version range " + range + " should be satisfied by " + matchingVersion,
                additionalRequirements.get(1).isMatch(iu));
    }

    private static IInstallableUnit createIU(String version) {
        InstallableUnitDescription iud = new InstallableUnitDescription();
        iud.setId(TARGET_UNIT_ID);
        Version osgiVersion = Version.create(version);
        iud.setVersion(osgiVersion);
        List<IProvidedCapability> list = new ArrayList<IProvidedCapability>();
        list.add(MetadataFactory.createProvidedCapability(IU_NAMESPACE, TARGET_UNIT_ID, osgiVersion));
        list.add(MetadataFactory.createProvidedCapability(BUNDLE_NAMESPACE, TARGET_UNIT_ID, osgiVersion));
        iud.addProvidedCapabilities(list);

        IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
        return iu;
    }
}
