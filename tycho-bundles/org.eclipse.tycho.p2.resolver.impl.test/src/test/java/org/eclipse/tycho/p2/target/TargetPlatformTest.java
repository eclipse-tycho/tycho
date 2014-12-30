/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createBundleIU;
import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createFeatureIU;
import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createProductIU;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TargetPlatformTest {

    private static final String ANY_VERSION = "0.0.0";

    @Rule
    public ExpectedException exceptions = ExpectedException.none();

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
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "1.1.0.v2013");

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
        assertThat(key.getId(), is("some.bundle"));
        assertThat(key.getVersion(), is("1.1.0.v2013"));
    }

    @Test
    public void testFailingResolve() throws Exception {
        exceptions.expect(not(isA(IllegalArtifactReferenceException.class))); // not a problem with the syntax
        exceptions.expectMessage("not found in the target platform");

        subject.resolveReference("eclipse-plugin", "other.bundle", "1.0.0");
    }

    @Test
    public void testResolveNullId() throws Exception {
        exceptions.expect(IllegalArtifactReferenceException.class);
        exceptions.expectMessage("ID is required");

        subject.resolveReference("eclipse-plugin", null, "1.0.0");
    }

    @Test
    public void testResolveInvalidVersionSyntax() throws Exception {
        exceptions.expect(IllegalArtifactReferenceException.class);
        exceptions.expectMessage("is not a valid OSGi version");

        subject.resolveReference("eclipse-plugin", "other.bundle", "1.0.0-SNAPSHOT");
    }

    @Test
    public void testResolveLatestVersionThroughZeros() throws Exception {
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "0.0.0");

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveLatestVersionThroughOmittedVersion() throws Exception {
        // e.g. when version attribute is omitted
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", null);

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveLatestQualifierWithQualifierLiteral() throws Exception {
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "1.1.0.qualifier");

        assertThat(key.getVersion(), is("1.1.0.v2014"));
    }

    @Test
    public void testResolveFixedVersionForThreeSegmentVersion() throws Exception {
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "1.1.0");

        // three-segment versions don't have a special semantic in the PDE, so 1.1.0 doesn't resolve to 1.1.0.v2014 (cf. bug 373844)
        assertThat(key.getVersion(), is("1.1.0"));
    }

    @Test
    public void testResolveBundleChecksType() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "1.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveReference("eclipse-plugin", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("1.0.0"));
    }

    @Test
    public void testResolveProduct() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveReference("eclipse-product", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PRODUCT));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("1.99.0"));
    }

    @Test
    public void testResolveIU() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveReference("p2-installable-unit", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_INSTALLABLE_UNIT));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("2.0.0"));
    }

    @Test
    public void testResolveFeature() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "2.0.0"), createBundleIU("unit.feature.group", "2.0.0"),
                createFeatureIU("unit", "1.2.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveReference("eclipse-feature", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_FEATURE));
        assertThat(key.getId(), is("unit")); // id is feature id
        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveUnknownType() throws Exception {
        exceptions.expect(IllegalArtifactReferenceException.class);
        exceptions.expectMessage("Unknown artifact type");

        subject.resolveReference("invalid-type", "unit", ANY_VERSION);
    }

    private FinalTargetPlatformImpl createTP() {
        return new FinalTargetPlatformImpl(candidateIUs, null, null, null, null, null);
    }

    private static LinkedHashSet<IInstallableUnit> createSet(IInstallableUnit... units) {
        return new LinkedHashSet<IInstallableUnit>(Arrays.asList(units));
    }
}
