/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.it.Verifier;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.EmptyResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class P2RepositoryMirrorTest extends AbstractTychoIntegrationTest {

	private HttpServer server;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	/**
	 * Tests whether Tycho is able to recover from a bad mirror repository. If
	 * multiple mirrors are specified for a repository, Tycho might be able to
	 * continue by requesting an artifact from a different mirror, depending on the
	 * error code returned by Equinox.
	 */
	@Test
	public void testMirrorWithRetry() throws Exception {
		Verifier verifier = getVerifier("p2Repository.mirror", false);

		String baseline = server.addServer("baseline", FirstBaselineRequestFailsServlet.class,
				new File(verifier.getBasedir(), "baseline"));
		// Two mirrors, to always have at least one that is still good
		String mirror1 = server.addServer("mirror1", FirstBaselineRequestFailsServlet.class,
				new File(verifier.getBasedir(), "baseline"));
		String mirror2 = server.addServer("mirror2", FirstBaselineRequestFailsServlet.class,
				new File(verifier.getBasedir(), "baseline"));
		String mirrors = baseline + '/' + "mirrors.xml";

		setMirrorsUrl(new File(verifier.getBasedir(), "baseline/artifacts.xml"), mirrors);
		setMirrors(new File(verifier.getBasedir(), "baseline/mirrors.xml"), mirror1, mirror2);

		// The verifier escapes the 'http://localhost' to 'http:/localhost'
		verifier.addCliOption("-Dbaseline=" + baseline.replaceAll("//", "////"));
		// Force an update of the HttpCache
		verifier.addCliOption("-U");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Artifact repository requested retry (attempt [1/3]):");
	}

	@Override
	protected boolean isDisableMirrors() {
		return false;
	}

	/**
	 * Updates the "p2.mirrorsURL" property in the {@code artifacts.xml} file of the
	 * baseline repository to point to the {@code mirrors.xml} file.
	 */
	private static void setMirrorsUrl(File artifactsXml, String mirrorsUrl) throws Exception {
		Document document = parseDocument(artifactsXml);

		XPath path = XPathFactory.newInstance().newXPath();
		String expression = "repository/properties/property[@name='p2.mirrorsURL']";
		Element node = (Element) path.evaluate(expression, document, XPathConstants.NODE);

		if (node != null) {
			node.setAttribute("value", mirrorsUrl);
			writeDocument(document, artifactsXml);
		}
	}

	/**
	 * Updates the {@link mirrors.xml} file to contain the bad mirror.
	 */
	private static void setMirrors(File mirrorsXml, String mirror1, String mirror2) throws Exception {
		Document document = parseDocument(mirrorsXml);

		XPath path = XPathFactory.newInstance().newXPath();
		String expression = "mirrors/mirror";
		NodeList nodes = (NodeList) path.evaluate(expression, document, XPathConstants.NODESET);

		if (nodes != null) {
			((Element) nodes.item(0)).setAttribute("url", mirror1);
			((Element) nodes.item(1)).setAttribute("url", mirror2);
			writeDocument(document, mirrorsXml);
		}
	}

	public static Document parseDocument(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(file);
	}

	public static void writeDocument(Document document, File file) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		try (FileWriter writer = new FileWriter(file)) {
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
		}
	}

	/**
	 * Helper servlet to simulate an illegal p2 repository. The first time a plugin
	 * is requested from the remote repository, a 404 is returned. Any further
	 * requests succeed, to test whether Tycho is able to recover from bad mirrors.
	 */
	public static class FirstBaselineRequestFailsServlet extends DefaultServlet {
		// We don't know which mirror is selected, so anyone can fail
		private static boolean firstRequest = true;

		@Override
		public Resource getResource(String pathInContext) {
			if (firstRequest && pathInContext.startsWith("/plugins/")) {
				firstRequest = false;
				return EmptyResource.INSTANCE;
			}
			return super.getResource(pathInContext);
		}
	}
}
