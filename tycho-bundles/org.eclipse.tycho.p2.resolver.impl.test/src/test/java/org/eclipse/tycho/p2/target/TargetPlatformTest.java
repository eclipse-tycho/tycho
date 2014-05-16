/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createBundleIU;
import static org.eclipse.tycho.p2.testutil.InstallableUnitUtil.createProductIU;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.junit.Before;
import org.junit.Ignore;
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
        candidateIUs = createSet(createBundleIU("some.bundle", "1.1.0.v2013"),
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
        exceptions.expectMessage("Cannot resolve reference");

        subject.resolveReference("eclipse-plugin", "other.bundle", "1.0.0");
    }

    @Ignore
    @Test
    public void testResolveInvalidVersionSyntax() throws Exception {
        // TODO
    }

    @Test
    public void testResolveLatestVersionThroughZeros() throws Exception {
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "0.0.0");

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveLatestVersionTrhoughEmptyString() throws Exception {
        // e.g. when version attribute is omitted
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "");

        assertThat(key.getVersion(), is("1.2.0"));
    }

    @Test
    public void testResolveVersionWithQualifierLiteral() throws Exception {
        ArtifactKey key = subject.resolveReference("eclipse-plugin", "some.bundle", "1.1.0.qualifier");

        assertThat(key.getVersion(), is("1.1.0.v2014"));
    }

    // TODO
    @Ignore
    @Test
    public void testResolveBundleChecksType() throws Exception {
        candidateIUs = createSet(createBundleIU("unit", "1.0.0"), createProductIU("unit", "1.99.0"));
        subject = createTP();

        ArtifactKey key = subject.resolveReference("eclipse-plugin", "unit", ANY_VERSION);

        assertThat(key.getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
        assertThat(key.getId(), is("unit"));
        assertThat(key.getVersion(), is("1.0.0"));
    }

    private FinalTargetPlatformImpl createTP() {
        return new FinalTargetPlatformImpl(candidateIUs, null, null, null, null, null);
    }

    private static LinkedHashSet<IInstallableUnit> createSet(IInstallableUnit... units) {
        return new LinkedHashSet<IInstallableUnit>(Arrays.asList(units));
    }
}
