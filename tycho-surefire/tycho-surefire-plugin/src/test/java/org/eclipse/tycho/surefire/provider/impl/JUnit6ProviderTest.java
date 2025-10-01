/*******************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Foundation - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class JUnit6ProviderTest extends AbstractJUnitProviderTest {

    @Test
    public void testEnabled() throws Exception {
        assertTrue(junitProvider.isEnabled(null, classPath("org.junit.jupiter.api:6.0.0"), new Properties()));
        assertTrue(junitProvider.isEnabled(null, classPath("junit-jupiter-api:6.1.0"), new Properties()));
        assertTrue(junitProvider.isEnabled(null, classPath("org.junit.jupiter.api:6.5.0"), new Properties()));
    }

    @Test
    public void testDisabled() throws Exception {
        assertFalse(junitProvider.isEnabled(null, classPath("foo:1.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(null, classPath("org.junit:4.8.2"), new Properties()));
        assertFalse(junitProvider.isEnabled(null, classPath("org.junit.jupiter.api:5.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(null, classPath("junit-jupiter-api:5.9.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(null, classPath("org.junit.jupiter.api:7.0"), new Properties()));
    }

    @Override
    protected AbstractJUnitProvider createJUnitProvider() {
        return new JUnit6Provider();
    }

}
