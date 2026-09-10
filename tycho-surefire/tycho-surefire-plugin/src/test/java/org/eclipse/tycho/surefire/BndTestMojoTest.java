/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class BndTestMojoTest {

    @Test
    public void testAddBndRunPropertiesTranslatesLeadingUnderscore() {
        Properties properties = new Properties();
        Map<String, String> bndRunProperties = new LinkedHashMap<>();
        bndRunProperties.put("_runsystemcapabilities", "osgi.wiring.host;osgi.wiring.host=org.eclipse.osgi");

        BndTestMojo.addBndRunProperties(properties, bndRunProperties);

        assertEquals("osgi.wiring.host;osgi.wiring.host=org.eclipse.osgi",
                properties.getProperty("-runsystemcapabilities"));
    }

    /**
     * A shared parent configuration may declare a bndRunProperties entry with an empty/placeholder
     * default value that is only meant to be overridden by specific modules (e.g. via
     * build.properties/pom.model.property). Maven/Plexus represents such blank Map values as
     * {@code null}. Verify that such entries are skipped instead of causing a NullPointerException
     * in {@link Properties#setProperty(String, String)}.
     */
    @Test
    public void testAddBndRunPropertiesSkipsNullValue() {
        Properties properties = new Properties();
        Map<String, String> bndRunProperties = new LinkedHashMap<>();
        bndRunProperties.put("_runsystemcapabilities", null);

        BndTestMojo.addBndRunProperties(properties, bndRunProperties);

        assertNull(properties.getProperty("-runsystemcapabilities"));
    }

    @Test
    public void testAddBndRunPropertiesSkipsBlankValue() {
        Properties properties = new Properties();
        Map<String, String> bndRunProperties = new LinkedHashMap<>();
        bndRunProperties.put("_runsystemcapabilities", "   ");

        BndTestMojo.addBndRunProperties(properties, bndRunProperties);

        assertNull(properties.getProperty("-runsystemcapabilities"));
    }
}
