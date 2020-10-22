/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class P2RepositoryPropertiesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testArtifactRepositoryExtraProperties() throws Exception {
        Verifier verifier = getVerifier("p2Repository.reactor", false);
        verifier.getSystemProperties().put("e352-repo", P2Repositories.ECLIPSE_352.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        File artifactXml = new File(verifier.getBasedir(), "eclipse-repository/target/repository/artifacts.xml");
        assertTrue(artifactXml.exists());
        Map<String, String> expected = new HashMap<>(3, 1.f);
        expected.put("p2.statsURI", "http://some.where");
        expected.put("p2.mirrorsURL", "http://some.where.else");
        expected.put("foo", "bar");
        Document artifactsDocument = XMLParser.parse(artifactXml);
        artifactsDocument.getChild("repository").getChild("properties").getChildren("property").forEach(element -> {
            String propertyName = element.getAttributeValue("name");
            if (expected.containsKey(propertyName)
                    && expected.get(propertyName).equals(element.getAttributeValue("value"))) {
                expected.remove(propertyName);
            }
        });
        assertEquals("Missing properties in artifact repository", Collections.emptyMap(), expected);

    }
}
