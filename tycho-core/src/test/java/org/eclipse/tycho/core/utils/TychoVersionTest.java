/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

public class TychoVersionTest {

    @Test
    public void testGetTychoVersion() throws IOException {
        assertEquals(readTestVersion(), TychoVersion.getTychoVersion());
    }

    private String readTestVersion() throws IOException {
        Properties p = new Properties();
        InputStream stream = TychoVersionTest.class.getResourceAsStream("testversion.properties");
        try {
            p.load(stream);
        } finally {
            IOUtil.close(stream);
        }
        return p.getProperty("testversion");
    }

}
