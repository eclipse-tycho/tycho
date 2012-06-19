/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class Junit3ProviderTest extends AbstractJUnitProviderTest {

    @Test
    public void testEnabled() throws Exception {
        assertTrue(junitProvider.isEnabled(classPath("org.junit:3.8.0"), new Properties()));
        assertTrue(junitProvider.isEnabled(classPath("org.junit:3.8.0", "org.junit:4.5.0"), new Properties()));
    }

    @Test
    public void testDisabled() throws Exception {
        assertFalse(junitProvider.isEnabled(classPath("foo:1.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit:4.0.0"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit:3.7"), new Properties()));
        assertFalse(junitProvider.isEnabled(classPath("org.junit4:3.8"), new Properties()));
    }

    @Override
    protected AbstractJUnitProvider createJUnitProvider() {
        return new JUnit3Provider();
    }
}
