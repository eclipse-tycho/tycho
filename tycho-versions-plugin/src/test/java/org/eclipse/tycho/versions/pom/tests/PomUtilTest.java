/*******************************************************************************
 * Copyright (c) 2024 SAP SE and others.
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
package org.eclipse.tycho.versions.pom.tests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.pom.PomUtil;
import org.junit.Test;

public class PomUtilTest {

    @Test
    public void expandProperties() throws Exception {
        String pom = """
                <project>
                  <properties>
                    <foo>    fooValue   </foo>
                    <bar>barValue</bar>
                  </properties>
                </project>
                """;
        PomFile pomFile = PomFile.read(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)), true);

        String expanded = PomUtil.expandProperties("${foo}-${bar}-${notFound}", pomFile.getProperties());

        assertEquals("fooValue-barValue-${notFound}", expanded);
    }
}
