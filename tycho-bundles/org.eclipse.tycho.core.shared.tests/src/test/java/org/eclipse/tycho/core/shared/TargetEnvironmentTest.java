/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
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
package org.eclipse.tycho.core.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
        assertEquals("(& (osgi.os=macosx) (osgi.ws=cocoa) (osgi.arch=ppc) )", subject.toFilterExpression());
    }

    @Test
    public void testToFilterExpressionWithUnsetAttribute() throws Exception {
        subject = new TargetEnvironment(OS, null, ARCH);
        assertEquals("(& (osgi.os=macosx) (osgi.arch=ppc) )", subject.toFilterExpression());
    }

    @Test
    public void testToFilterExpressionWithOnlyOneAttribute() throws Exception {
        subject = new TargetEnvironment(OS, null, null);
        assertEquals("(osgi.os=macosx)", subject.toFilterExpression());
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
        assertNotEquals(left, right);

        // should also not lead to hash code collisions
        assertNotEquals(left.hashCode(), right.hashCode());
    }
}
