/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.facade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TargetEnvironmentTest {
    private static final String OS = "macosx";
    private static final String WS = "cocoa";
    private static final String ARCH = "ppc";

    private TargetEnvironment subject;

    @Before
    public void initSubject() {
        subject = new TargetEnvironment(OS, WS, ARCH);
    }

    @Test
    public void testGetters() {
        assertEquals(OS, subject.getOs());
        assertEquals(WS, subject.getWs());
        assertEquals(ARCH, subject.getArch());
    }

    @Test
    public void testToConfigSpec() {
        assertEquals("cocoa.macosx.ppc", subject.toConfigSpec());
    }

    @Test
    public void testToFilterProperties() {
        Map<String, String> filterMap = subject.toFilterProperties();

        assertEquals(3, filterMap.size());
        assertEquals(OS, filterMap.get("osgi.os"));
        assertEquals(WS, filterMap.get("osgi.ws"));
        assertEquals(ARCH, filterMap.get("osgi.arch"));
    }

    @Test
    public void testToFilterExpression() throws Exception {
        assertThat(subject.toFilterExpression(), is("(& (osgi.os=macosx) (osgi.ws=cocoa) (osgi.arch=ppc) )"));
    }

    @Test
    public void testToFilterExpressionWithUnsetAttribute() throws Exception {
        subject = new TargetEnvironment(OS, null, ARCH);
        assertThat(subject.toFilterExpression(), is("(& (osgi.os=macosx) (osgi.arch=ppc) )"));
    }

    @Test
    public void testToFilterExpressionWithOnlyOneAttribute() throws Exception {
        subject = new TargetEnvironment(OS, null, null);
        assertThat(subject.toFilterExpression(), is("(osgi.os=macosx)"));
    }

    @Test
    public void testEquals() {
        TargetEnvironment equalInstance = new TargetEnvironment("macosx", "cocoa", "ppc");
        assertEquals(subject, equalInstance);
        assertEquals(subject.hashCode(), equalInstance.hashCode());
    }

    @Test
    public void testNonEquals() throws Exception {
        asserNotEqual(subject, new TargetEnvironment(null, WS, ARCH));
        asserNotEqual(subject, new TargetEnvironment(OS, null, ARCH));
        asserNotEqual(subject, new TargetEnvironment(OS, WS, null));
    }

    private static void asserNotEqual(TargetEnvironment left, TargetEnvironment right) {
        assertThat(left, not(right));

        // should also not lead to hash code collisions
        assertThat(left.hashCode(), not(right.hashCode()));
    }
}
