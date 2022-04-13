/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
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
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createBundleIU;
import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createFeatureIU;
import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createProductIU;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.DependencyResolutionException;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.junit.Before;
import org.junit.Test;

public class TargetPlatformTest {

    private static final String ANY_VERSION = "0.0.0";

    private TargetPlatform subject;
    private LinkedHashSet<IInstallableUnit> candidateIUs;

    @Before
    public void initDefaultTestData() {
        candidateIUs = createSet(createBundleIU("some.bundle", "1.1.0"), createBundleIU("some.bundle", "1.1.0.v2013"),
                createBundleIU("some.bundle", "1.1.0.v2014"), createBundleIU("some.bundle", "1.2.0"),
                createBundleIU("other.bundle", "1.2.99"));
        subject = createTP();
    }

    @Test
    public void testResolveFixedVersion() throws Exception {
        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "some.bundle", "1.1.0.v2013");

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
        assertThat(key.getId(), is("some.bundle"));
        assertThat(key.getVersion(), is("1.1.0.v2013"));
    }

    @Test
    public void testFailingResolve() throws Exception {
        Exception e = assertThrows(Exception.class,
                () -> subject.resolveArtifact("eclipse-plugin", "other.bundle", "1.0.0"));
        assertFalse(e instanceof IllegalArtifactReferenceException);
        assertTrue(e.getMessage().contains("not found in the target platform"));
    }

    @Test
    public void testResolveNullId() throws Exception {
        IllegalArtifactReferenceException e = assertThrows(IllegalArtifactReferenceException.class,
                () -> subject.resolveArtifact("eclipse-plugin", null, "1.0.0"));
        assertTrue(e.getMessage().contains("ID is required"));
    }

    @Test
    public void testResolveInvalidVersionSyntax() throws Exception {
        IllegalArtifactReferenceException e = assertThrows(IllegalArtifactReferenceException.class,
                () -> subject.resolveArtifact("eclipse-plugin", "other.bundle", "1.0.0-SNAPSHOT"));
        assertTrue(e.getMessage().contains("is not a valid OSGi version"));
    }

    @Test
    public void testResolveLatestVersionThroughZeros() throws Exception {
        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "some.bundle", "0.0.0");

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveLatestVersionThroughOmittedVersion() throws Exception {
        // e.g. when version attribute is omitted
        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "some.bundle", null);

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveLatestQualifierWithQualifierLiteral() throws Exception {
        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "some.bundle", "1.1.0.qualifier");

        assertThat(key.getVersion(), is("1.1.0.v2014"));
    }

    @Test
    public void testResolveFixedVersionForThreeSegmentVersion() throws Exception {
        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "some.bundle", "1.1.0");

        // three-segment versions don't have a special semantic in the PDE, so 1.1.0 doesn't resolve to 1.1.0.v2014 (cf. bug 373844)
        assertThat(key.getVersion(), is("1.1.0"));
    }

    @Test
    public void testResolveBundleChecksType() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "1.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveArtifact("eclipse-plugin", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("1.0.0"));
    }

    @Test
    public void testResolveProduct() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveArtifact("eclipse-product", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PRODUCT));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("1.99.0"));
    }

    @Test
    public void testResolveIU() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveArtifact("p2-installable-unit", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_INSTALLABLE_UNIT));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("2.0.0"));
    }

    @Test
    public void testResolveFeature() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createBundleIU("unit.feature.group", "2.0.0"),
                createFeatureIU("unit", "1.2.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveArtifact("eclipse-feature", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_FEATURE));
        assertThat(key.getId(), is("unit")); // id is feature id
        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveUnknownType() throws Exception {
        DependencyResolutionException e = assertThrows(DependencyResolutionException.class,
                () -> subject.resolveArtifact("invalid-type", "unit", ANY_VERSION));
        assertTrue(e.getMessage().contains("invalid-type"));
    }

    private FinalTargetPlatformImpl createTP() {
        return new FinalTargetPlatformImpl(candidateIUs, null, null, null, null, null);
    }

    private static LinkedHashSet<IInstallableUnit> createSet(IInstallableUnit... units) {
        return new LinkedHashSet<>(Arrays.asList(units));
    }
}
