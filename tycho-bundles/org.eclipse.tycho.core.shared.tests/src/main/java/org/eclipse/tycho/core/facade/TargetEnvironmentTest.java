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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TargetEnvironmentTest {
    private static final String OS = "mac";
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
        assertEquals("cocoa.mac.ppc", subject.toConfigSpec());
    }

    @Test
    public void testToFilter() {
        Map<String, String> filterMap = subject.toFilter();

        assertEquals(3, filterMap.size());
        assertEquals(OS, filterMap.get("osgi.os"));
        assertEquals(WS, filterMap.get("osgi.ws"));
        assertEquals(ARCH, filterMap.get("osgi.arch"));
    }
}
