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
package org.eclipse.tycho.p2.target.ee;

import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.eeCapability;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.packageCapability;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unit;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.hamcrest.Matcher;
import org.junit.Test;

public class StandardEEResolutionHintsTest {

    private StandardEEResolutionHints subject;
    private IInstallableUnit jreUnit;

    @Test
    public void testJava8() {
        subject = new StandardEEResolutionHints("JavaSE-1.8");

        assertThat(subject.getMandatoryUnits(), hasItem(unit("a.jre.javase", "1.8.0")));
        assertThat(subject.getMandatoryUnits(), hasItem(unit("config.a.jre.javase", "1.8.0")));
        assertThat(subject.getMandatoryUnits().size(), is(2));

        jreUnit = findFirst(unit("a.jre.javase", "1.8.0"), subject.getMandatoryUnits());
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.xml")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.xml.ws.spi.http")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE", "1.8.0")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE/compact1", "1.8.0")));
    }

    @Test
    public void testJava8Compact1() {
        subject = new StandardEEResolutionHints("JavaSE/compact1-1.8");

        assertThat(subject.getMandatoryUnits(), hasItem(unit("a.jre.javase.compact1", "1.8.0")));
        assertThat(subject.getMandatoryUnits(), hasItem(unit("config.a.jre.javase.compact1", "1.8.0")));
        assertThat(subject.getMandatoryUnits().size(), is(2));

        jreUnit = findFirst(unit("a.jre.javase.compact1", "1.8.0"), subject.getMandatoryUnits());
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.net")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE/compact1", "1.8.0")));

        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(packageCapability("javax.xml"))));
        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(eeCapability("JavaSE", "1.8.0"))));
    }

    @Test
    public void testJava7() {
        subject = new StandardEEResolutionHints("JavaSE-1.7");

        assertThat(subject.getMandatoryUnits(), hasItem(unit("a.jre.javase", "1.7.0")));
        assertThat(subject.getMandatoryUnits(), hasItem(unit("config.a.jre.javase", "1.7.0")));
        assertThat(subject.getMandatoryUnits().size(), is(2));

        jreUnit = findFirst(unit("a.jre.javase", "1.7.0"), subject.getMandatoryUnits());
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.xml")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.xml.ws.spi.http")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE", "1.6.0")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE", "1.7.0")));

        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(eeCapability("JavaSE", "1.8.0"))));
        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(eeCapability("JavaSE/compact1", "1.8.0"))));
    }

    @Test
    public void testJava6() {
        subject = new StandardEEResolutionHints("JavaSE-1.6");

        assertThat(subject.getMandatoryUnits(), hasItem(unit("a.jre.javase", "1.6.0")));
        assertThat(subject.getMandatoryUnits(), hasItem(unit("config.a.jre.javase", "1.6.0")));
        assertThat(subject.getMandatoryUnits().size(), is(2));

        jreUnit = findFirst(unit("a.jre.javase", "1.6.0"), subject.getMandatoryUnits());
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(packageCapability("javax.xml")));
        assertThat(jreUnit.getProvidedCapabilities(), hasItem(eeCapability("JavaSE", "1.6.0")));

        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(packageCapability("javax.xml.ws.spi.http"))));
        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(eeCapability("JavaSE", "1.7.0"))));
    }

    @Test(expected = RuntimeException.class)
    public void testNoSilentFallBackToJava6() {
        String wrongEE = "JavaSE-1.5"; // Java 5 is called "J2SE-1.5"
        subject = new StandardEEResolutionHints(wrongEE);
    }

    @Test
    public void testTemporaryJava6StubUnitForOtherEEs() {
        subject = new StandardEEResolutionHints("JavaSE-1.7");

        // we temporarily add a.jre.javase/1.6.0 units during resolution because products often have hard requirements on them...
        assertThat(subject.getTemporaryAdditions(), hasItem(unit("a.jre.javase", "1.6.0")));
        assertThat(subject.getTemporaryAdditions(), hasItem(unit("config.a.jre.javase", "1.6.0")));

        // ... but the fake units are empty
        jreUnit = findFirst(unit("a.jre.javase", "1.6.0"), subject.getTemporaryAdditions());
        assertThat(jreUnit.getProvidedCapabilities(), not(hasItem(packageCapability("javax.xml"))));
        assertThat(jreUnit.getProvidedCapabilities().size(), is(1)); // the self-capability
    }

    @Test
    public void testNoTemporaryJava6StubUnitForJava6() {
        subject = new StandardEEResolutionHints("JavaSE-1.6");

        assertThat(subject.getTemporaryAdditions(), not(hasItem(unit("a.jre.javase", "1.6.0"))));
        assertThat(subject.getTemporaryAdditions(), not(hasItem(unit("config.a.jre.javase", "1.6.0"))));
    }

    private static IInstallableUnit findFirst(Matcher<IInstallableUnit> criteria, Collection<IInstallableUnit> inUnits) {
        for (IInstallableUnit unit : inUnits) {
            if (criteria.matches(unit)) {
                return unit;
            }
        }
        throw new IllegalStateException();
    }

}
