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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractJUnitProviderTest {

    protected AbstractJUnitProvider junitProvider;

    public AbstractJUnitProviderTest() {
    }

    protected abstract AbstractJUnitProvider createJUnitProvider();

    @Before
    public void setup() {
        junitProvider = createJUnitProvider();
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals("junit", junitProvider.getType());
    }

    static List<ClasspathEntry> classPath(String... entries) {
        List<ClasspathEntry> result = new ArrayList<>();
        for (String entry : entries) {
            int colonIndex = entry.indexOf(':');
            assertNotSame(-1, colonIndex);
            String id = entry.substring(0, colonIndex);
            String version = entry.substring(colonIndex + 1);
            result.add(new DefaultClasspathEntry(null, new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, id,
                    version), null, null));
        }
        return result;
    }

}
