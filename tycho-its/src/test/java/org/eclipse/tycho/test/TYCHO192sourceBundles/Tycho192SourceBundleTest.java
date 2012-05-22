/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO192sourceBundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Tycho192SourceBundleTest extends AbstractTychoIntegrationTest {

    private final DocumentBuilder docBuilder = createDocBuilder();
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    private DocumentBuilder createDocBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDefaultSourceBundleSuffix() throws Exception {
        Verifier verifier = getVerifier("/TYCHO192sourceBundles", false);
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        assertUpdateSiteContainsSourceJar(verifier);
        File bundleTargetDir = new File(verifier.getBasedir(), "helloworld/target/");
        checkP2ArtifactsXml(new File(bundleTargetDir, RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS));
        checkP2ContentXml(new File(bundleTargetDir, RepositoryLayoutHelper.FILE_NAME_P2_METADATA));
    }

    private void checkP2ContentXml(File p2Content) throws Exception {
        assertTrue(p2Content.isFile());
        Document p2ContentDOM = docBuilder.parse(p2Content);
        XPathExpression sourceBundleUnitExpression = xpath.compile("/units/unit[@id = 'helloworld.source']");
        Element sourceBundleUnitNode = (Element) sourceBundleUnitExpression.evaluate(p2ContentDOM.getDocumentElement(),
                XPathConstants.NODE);
        assertNotNull("unit with id 'helloworld.source' not found", sourceBundleUnitNode);
        assertHasMavenClassifierProperty(sourceBundleUnitNode);
    }

    private void assertHasMavenClassifierProperty(Element node) throws XPathExpressionException {
        XPathExpression classifierNodeExpression = xpath.compile("properties/property[@name = 'maven-classifier']");
        Element classifierNode = (Element) classifierNodeExpression.evaluate(node, XPathConstants.NODE);
        assertNotNull("property node with name 'maven-classifier' not found", classifierNode);
        assertEquals("sources", classifierNode.getAttribute("value"));
    }

    private void checkP2ArtifactsXml(File p2Artifacts) throws SAXException, IOException, ParserConfigurationException,
            XPathExpressionException {
        assertTrue(p2Artifacts.isFile());
        Document p2ArtifactsDOM = docBuilder.parse(p2Artifacts);
        XPathExpression sourceBundleNodeExpression = xpath.compile("/artifacts/artifact[@id = 'helloworld.source']");
        Element sourceBundleArtifactNode = (Element) sourceBundleNodeExpression.evaluate(
                p2ArtifactsDOM.getDocumentElement(), XPathConstants.NODE);
        assertNotNull("artifact with id 'helloworld.source' not found", sourceBundleArtifactNode);
        assertHasMavenClassifierProperty(sourceBundleArtifactNode);
    }

    private void assertUpdateSiteContainsSourceJar(Verifier verifier) throws IOException {
        File[] sourceJars = new File(verifier.getBasedir(), "helloworld.updatesite/target/site/plugins")
                .listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isFile() && pathname.getName().startsWith("helloworld.source_");
                    }
                });
        assertEquals(1, sourceJars.length);
        JarFile sourceJar = new JarFile(sourceJars[0]);
        try {
            assertNotNull(sourceJar.getEntry("helloworld/MessageProvider.java"));
            Attributes sourceBundleHeaders = sourceJar.getManifest().getMainAttributes();
            assertEquals("%bundleName", sourceBundleHeaders.getValue(Constants.BUNDLE_NAME));
            assertEquals("%bundleVendor", sourceBundleHeaders.getValue(Constants.BUNDLE_VENDOR));
            assertEquals("OSGI-INF/l10n/bundle-src", sourceBundleHeaders.getValue(Constants.BUNDLE_LOCALIZATION));
            ZipEntry l10nPropsEntry = sourceJar.getEntry("OSGI-INF/l10n/bundle-src.properties");
            assertNotNull(l10nPropsEntry);
            Properties l10nProps = new Properties();
            InputStream propsStream = sourceJar.getInputStream(l10nPropsEntry);
            l10nProps.load(propsStream);
            assertEquals(2, l10nProps.size());
            assertEquals("Hello Plugin Source", l10nProps.getProperty("bundleName"));
            assertEquals("Hello Vendor", l10nProps.getProperty("bundleVendor"));
        } finally {
            sourceJar.close();
        }
    }
}
