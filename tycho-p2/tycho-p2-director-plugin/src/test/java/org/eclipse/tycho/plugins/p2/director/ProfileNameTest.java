/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
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
package org.eclipse.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.junit.Test;

public class ProfileNameTest {

    private static final String DEFAULT_NAME = "TestProfileName";

    private static final ProfileName SPECIFIC_LINUX_CONFIG = new ProfileName("specific-profile", "linux", "gtk",
            "x86_64");
    private static final ProfileName GENERAL_LINUX_CONFIG = new ProfileName("linux-profile", "linux", null, null);

    private static final TargetEnvironment LINUX_GTK_X86_64 = new TargetEnvironment("linux", "gtk", "x86_64");
    private static final TargetEnvironment WIN32_WIN32_X86_64 = new TargetEnvironment("win32", "win32", "x86_64");

    @Test
    public void testNoEnvironmentSpecificNames() throws Exception {
        assertEquals(DEFAULT_NAME, ProfileName.getNameForEnvironment(LINUX_GTK_X86_64, null, DEFAULT_NAME));
    }

    @Test
    public void testExactMatch() throws Exception {
        List<ProfileName> configuration = Arrays.asList(SPECIFIC_LINUX_CONFIG);
        String name = ProfileName.getNameForEnvironment(LINUX_GTK_X86_64, configuration, DEFAULT_NAME);
        assertEquals(SPECIFIC_LINUX_CONFIG.getName(), name);
    }

    @Test
    public void testNoMatch() {
        List<ProfileName> configuration = Arrays.asList(SPECIFIC_LINUX_CONFIG);
        String name = ProfileName.getNameForEnvironment(WIN32_WIN32_X86_64, configuration, DEFAULT_NAME);
        assertEquals(DEFAULT_NAME, name);
    }

    @Test
    public void testMatchWithPartialEnvironment() {
        List<ProfileName> configuration = Arrays.asList(GENERAL_LINUX_CONFIG);
        String name = ProfileName.getNameForEnvironment(LINUX_GTK_X86_64, configuration, DEFAULT_NAME);
        assertEquals(GENERAL_LINUX_CONFIG.getName(), name);
    }

    @Test
    public void testFirstMatchIsUsed() throws Exception {
        List<ProfileName> configuration = Arrays.asList(GENERAL_LINUX_CONFIG, SPECIFIC_LINUX_CONFIG);
        String name = ProfileName.getNameForEnvironment(LINUX_GTK_X86_64, configuration, DEFAULT_NAME);
        assertEquals(GENERAL_LINUX_CONFIG.getName(), name);
    }
}
