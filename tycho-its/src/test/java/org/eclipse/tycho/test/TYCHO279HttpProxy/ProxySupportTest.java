/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO279HttpProxy;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.jettytestsuite.ProxyServer;
import org.sonatype.jettytestsuite.proxy.MonitorableProxyServlet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProxySupportTest extends AbstractTychoIntegrationTest {

    private static final String TEST_BASEDIR = "/TYCHO279HttpProxy";

    private static final String PATH = "/test/";

    private Server httpServer;

    private int httpServerPort;

    private ProxyServer proxyServer;

    private int proxyPort;

    private MonitorableProxyServlet proxyServlet;

    private File baseDir;

    private File settings;

    @Before
    public void setup() throws Exception {
        baseDir = new File(getVerifier(TEST_BASEDIR).getBasedir());
        settings = new File(baseDir, "settings.xml");
        startHttpServer();
    }

    @After
    public void tearDown() throws Exception {
        httpServer.stop();
        httpServer.join();
        proxyServer.getServer().stop();
        proxyServer.getServer().join();
    }

    @Test
    public void testActiveProxy() throws Exception {
        startHttpProxyServer(false, null, null);
        Verifier verifier = getVerifier(TEST_BASEDIR, false);
        configureProxyInSettingsXml(true, null, null);
        replaceSettingsArg(verifier);
        verifier.getSystemProperties().setProperty("p2.repo", getP2RepoUrl());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        List<String> accessedUris = proxyServlet.getAccessedUris();
        Assert.assertTrue("proxy was not accessed", accessedUris.size() > 0);
        String expectedUri = getP2RepoUrl() + "artifacts.xml";
        Assert.assertTrue("URL " + expectedUri + " was not accessed via proxy", accessedUris.contains(expectedUri));
    }

    @Test
    public void testProxyWithAuthentication() throws Exception {
        final String proxyUser = "foo";
        final String proxyPassword = "bar";
        startHttpProxyServer(true, proxyUser, proxyPassword);
        Verifier verifier = getVerifier(TEST_BASEDIR, false);
        configureProxyInSettingsXml(true, proxyUser, proxyPassword);
        replaceSettingsArg(verifier);
        verifier.getSystemProperties().setProperty("p2.repo", getP2RepoUrl());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        List<String> accessedUris = proxyServlet.getAccessedUris();
        Assert.assertTrue("proxy was not accessed", accessedUris.size() > 0);
        String expectedUri = getP2RepoUrl() + "artifacts.xml";
        Assert.assertTrue("URL " + expectedUri + " was not accessed via proxy", accessedUris.contains(expectedUri));
    }

    @Test
    public void testInactiveProxy() throws Exception {
        startHttpProxyServer(false, null, null);
        Verifier verifier = getVerifier(TEST_BASEDIR, false);
        configureProxyInSettingsXml(false, null, null);
        replaceSettingsArg(verifier);
        verifier.getSystemProperties().setProperty("p2.repo", getP2RepoUrl());
        verifier.executeGoal("package"); // build fails
        List<String> accessedUris = proxyServlet.getAccessedUris();
        Assert.assertTrue("proxy was accessed although not active. Accessed URIs: " + accessedUris,
                accessedUris.size() == 0);
    }

    private void startHttpProxyServer(boolean useAuthentication, String user, String password) throws Exception {
        proxyServer = new ProxyServer();
        proxyPort = findFreePort();
        proxyServer.setPort(proxyPort);
        proxyServer.initialize();

        Map<String, String> authMap = new HashMap<String, String>();
        if (useAuthentication) {
            authMap.put(user, password);
        }
        proxyServlet = proxyServer.getProxyServlet();
        proxyServlet.setAuthentications(authMap);
        proxyServlet.setUseAuthentication(useAuthentication);

        proxyServer.start();
    }

    private void startHttpServer() throws Exception {
        httpServer = new Server();
        Connector connector = new SocketConnector();
        httpServerPort = findFreePort();
        connector.setPort(httpServerPort);
        httpServer.addConnector(connector);
        ContextHandler context = new ContextHandler(httpServer, "/test");
        context.setResourceBase(new File(baseDir, "repo").getAbsolutePath());
        context.setHandler(new ResourceHandler());
        httpServer.setHandler(context);
        httpServer.start();
    }

    private static int findFreePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }

    private String getP2RepoUrl() {
        return "http://localhost:" + httpServerPort + PATH;
    }

    private void replaceSettingsArg(Verifier verifier) throws IOException {
        List<String> cliOptions = verifier.getCliOptions();
        for (Iterator<String> iterator = cliOptions.iterator(); iterator.hasNext();) {
            String arg = iterator.next();
            if (arg.startsWith("-s ")) {
                iterator.remove();
            }
        }
        cliOptions.add("-s " + settings.getCanonicalPath());
    }

    private void configureProxyInSettingsXml(boolean isProxyActive, String user, String password) throws Exception {
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(settings);
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression proxyExpr = xpath.compile("/settings/proxies/proxy");
        Element proxyNode = (Element) proxyExpr.evaluate(dom.getDocumentElement(), XPathConstants.NODE);
        {
            XPathExpression portExpr = xpath.compile("/settings/proxies/proxy/port");
            Element node = (Element) portExpr.evaluate(dom.getDocumentElement(), XPathConstants.NODE);
            node.setTextContent(String.valueOf(proxyPort));
        }
        {
            XPathExpression activeExpr = xpath.compile("/settings/proxies/proxy/active");
            Element activeNode = (Element) activeExpr.evaluate(dom.getDocumentElement(), XPathConstants.NODE);
            activeNode.setTextContent(String.valueOf(isProxyActive));
        }
        {
            XPathExpression userExpr = xpath.compile("/settings/proxies/proxy/username");
            Element userNode = (Element) userExpr.evaluate(dom.getDocumentElement(), XPathConstants.NODE);
            updateNodeValue("username", userNode, user, dom, proxyNode);
        }
        {
            XPathExpression passwordExpr = xpath.compile("/settings/proxies/proxy/password");
            Element passwordNode = (Element) passwordExpr.evaluate(dom.getDocumentElement(), XPathConstants.NODE);
            updateNodeValue("password", passwordNode, password, dom, proxyNode);
        }
        Transformer xslTransformer = TransformerFactory.newInstance().newTransformer();
        xslTransformer.transform(new DOMSource(dom.getDocumentElement()), new StreamResult(settings));
    }

    private static void updateNodeValue(String nodeName, Element node, String newValue, Document dom, Element parentNode) {
        if (newValue != null) {
            if (node == null) {
                node = dom.createElement(nodeName);
                parentNode.appendChild(node);
            }
            node.setTextContent(newValue);
        } else {
            if (node != null) {
                parentNode.removeChild(node);
            }
        }
    }

}
