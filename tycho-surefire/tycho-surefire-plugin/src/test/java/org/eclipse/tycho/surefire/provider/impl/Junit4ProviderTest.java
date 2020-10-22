/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class Junit4ProviderTest extends AbstractJUnitProviderTest {

    @Test
    public void testEnabled() throws Exception {
        assertTrue(junitProvider.isEnabled(classPath("org.junit:4.0"), new Properties()));
        assertTrue(junitProvider.isEnabled(classPath("org.junit4:4.8.1"), new Properties()));
        assertTrue(junitProvider.isEnabled(classPath("org.junit:3.8.2", "org.junit:4.5.0"), new Properties()));
    }

    @Test
    public void testDisabled() throws Exception {
        assertFalse(junitProvider.isEnabled(classPath(), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("foo:1.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit:3.8.2"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit:5.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit4:5.1"), new Properties()));
    }

    @Override
    protected AbstractJUnitProvider createJUnitProvider() {
        return new JUnit4Provider();
    }
}
