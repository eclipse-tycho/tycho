/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.eclipse.tycho.versions.bundle.MutableBndFile;
import org.junit.Test;
import org.osgi.framework.Constants;

public class MutableBndFileTest {

    @Test
    public void linebreakTest() throws IOException {
        MutableBndFile bnd = getBndFile("linebreak");
        //bnd.setValue(Constants.BUNDLE_VERSION, "xxx");
        StringWriter writer = new StringWriter();
        bnd.write(writer);
        assertEquals(getBndString("linebreak"), writer.toString());
    }

    @Test
    public void versionTest() throws IOException {
        String newVersion = "xxx";
        MutableBndFile bnd = getBndFile("linebreak");
        bnd.setValue(Constants.BUNDLE_VERSION, newVersion);
        StringWriter writer = new StringWriter();
        bnd.write(writer);
        assertEquals(getBndString("linebreak").replace("1.0.0.qualifier", newVersion), writer.toString());
    }

    private MutableBndFile getBndFile(String filename) throws IOException {
        return MutableBndFile.read(getBndStream(filename));
    }

    private String getBndString(String filename) throws IOException {
        return new String(getBndStream(filename).readAllBytes());
    }

    private InputStream getBndStream(String filename) {
        return getClass().getResourceAsStream("/bnds/" + filename + ".bnd");
    }
}
