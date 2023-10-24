/*******************************************************************************
 * Copyright (c) 2010, 2023 SAP AG and others.
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

import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.XMLTool;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Tycho192SourceBundleTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultSourceBundleSuffix() throws Exception {
		Verifier verifier = getVerifier("/TYCHO192sourceBundles", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		assertUpdateSiteContainsSourceJar(verifier);
		File bundleTargetDir = new File(verifier.getBasedir(), "helloworld/target/");
		checkP2ArtifactsXml(new File(bundleTargetDir, TychoConstants.FILE_NAME_P2_ARTIFACTS));
		checkP2ContentXml(new File(bundleTargetDir, TychoConstants.FILE_NAME_P2_METADATA));
	}

	private void checkP2ContentXml(File p2Content) throws Exception {
		assertTrue(p2Content.isFile());
		Document p2ContentDOM = XMLTool.parseXMLDocument(p2Content);
		Element sourceBundleUnitNode = (Element) XMLTool.getFirstMatchingNode(p2ContentDOM,
				"/units/unit[@id = 'helloworld.source']");
		assertNotNull("unit with id 'helloworld.source' not found", sourceBundleUnitNode);
		assertHasMavenClassifierProperty(sourceBundleUnitNode);
	}

	private void assertHasMavenClassifierProperty(Element node) throws XPathExpressionException {
		Element classifierNode = (Element) XMLTool.getFirstMatchingNode(node,
				"properties/property[@name = 'maven-classifier']");
		assertNotNull("property node with name 'maven-classifier' not found", classifierNode);
		assertEquals("sources", classifierNode.getAttribute("value"));
	}

	private void checkP2ArtifactsXml(File p2Artifacts) throws Exception {
		assertTrue(p2Artifacts.isFile());
		Document p2ArtifactsDOM = XMLTool.parseXMLDocument(p2Artifacts);
		Element sourceBundleArtifactNode = (Element) XMLTool.getFirstMatchingNode(p2ArtifactsDOM,
				"/artifacts/artifact[@id = 'helloworld.source']");
		assertNotNull("artifact with id 'helloworld.source' not found", sourceBundleArtifactNode);
		assertHasMavenClassifierProperty(sourceBundleArtifactNode);
	}

	private void assertUpdateSiteContainsSourceJar(Verifier verifier) throws IOException {
		File[] sourceJars = new File(verifier.getBasedir(), "helloworld.updatesite/target/repository/plugins")
				.listFiles((FileFilter) pathname -> pathname.isFile()
						&& pathname.getName().startsWith("helloworld.source_"));
		assertEquals(1, sourceJars.length);
		try (JarFile sourceJar = new JarFile(sourceJars[0])) {
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
		}
	}
}
