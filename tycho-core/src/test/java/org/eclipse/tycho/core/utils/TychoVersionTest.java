/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
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

package org.eclipse.tycho.core.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.tycho.version.TychoVersion;
import org.junit.Test;

public class TychoVersionTest {

    @Test
    public void testGetTychoVersion() throws IOException {
        assertEquals(readTestVersion(), TychoVersion.getTychoVersion());
    }

    private String readTestVersion() throws IOException {
        Properties p = new Properties();
        try (InputStream stream = TychoVersionTest.class.getResourceAsStream("testversion.properties")) {
            p.load(stream);
        }
        return p.getProperty("testversion");
    }

}
