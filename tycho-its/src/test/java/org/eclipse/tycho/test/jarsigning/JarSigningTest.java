/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.jarsigning;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JarSigningTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSigning() throws Exception {
        Verifier verifier = getVerifier("jar-signing", false);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("verified successfully");
        checkMD5SumsArePresent(verifier);
    }

    @Test
    public void testExtraSigning() throws Exception {
        Verifier verifier = getVerifier("jar-signing-extra", false);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("verified successfully");
        checkMD5SumsArePresent(verifier);
    }

    private void checkMD5SumsArePresent(Verifier verifier) throws Exception {
        File repoDir = new File(verifier.getBasedir(), "rcp/target/repository");
        File artifacts = new File(repoDir, "artifacts.jar");
        Assert.assertTrue(artifacts.isFile());
        DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = null;
        ZipFile artifactsJar = new ZipFile(artifacts);
        try {
            ZipEntry artifactsXmlEntry = artifactsJar.getEntry("artifacts.xml");
            document = parser.parse(artifactsJar.getInputStream(artifactsXmlEntry));
        } finally {
            artifactsJar.close();
        }
        Element repository = document.getDocumentElement();
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xpath.evaluate("/repository/artifacts/artifact", repository,
                XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element artifactNode = (Element) nodeList.item(i);
            NodeList properties = (NodeList) xpath.evaluate("properties/property", artifactNode,
                    XPathConstants.NODESET);
            boolean hasMD5 = false;
            for (int j = 0; j < properties.getLength(); j++) {
                Element property = (Element) properties.item(j);
                String propName = property.getAttribute("name");
                if ("download.md5".equals(propName)) {
                    hasMD5 = true;
                    break;
                }
            }
            Assert.assertTrue("bug 357513 - artifact does not have a 'download.md5' attribute", hasMD5);
        }
    }
}
