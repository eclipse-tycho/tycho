/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLTool {
    private XMLTool() { // static use only
    }

    private static final DocumentBuilderFactory FACTORY;
    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // completely disable external entities declarations:
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        FACTORY = factory;
    }

    public static Document parseXMLDocument(File file) throws SAXException, IOException, ParserConfigurationException {
        return FACTORY.newDocumentBuilder().parse(file);
    }

    public static Document parseXMLDocumentFromJar(File jarFile, String entryPath)
            throws SAXException, IOException, ParserConfigurationException {
        try (JarFile jar = new JarFile(jarFile); //
                InputStream stream = jar.getInputStream(jar.getEntry(entryPath));) {
            return FACTORY.newDocumentBuilder().parse(stream);
        }
    }

    private static final ThreadLocal<XPath> XPATH_TOOL = ThreadLocal
            .withInitial(() -> XPathFactory.newInstance().newXPath());

    private static Object evaluateXPath(Object startingPoint, String xpathExpression, QName returnType)
            throws XPathExpressionException {
        return XPATH_TOOL.get().evaluate(xpathExpression, startingPoint, returnType);
    }

    public static Node getFirstMatchingNode(Object startingPoint, String xpathExpression)
            throws XPathExpressionException {
        return (Node) evaluateXPath(startingPoint, xpathExpression, XPathConstants.NODE);
    }

    public static List<Node> getMatchingNodes(Object startingPoint, String xpathExpression)
            throws XPathExpressionException {
        NodeList nodeList = (NodeList) evaluateXPath(startingPoint, xpathExpression, XPathConstants.NODESET);
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).toList();
    }

    public static List<String> getMatchingNodesValue(Object startingPoint, String xpathExpression)
            throws XPathExpressionException {
        return getMatchingNodes(startingPoint, xpathExpression).stream().map(Node::getNodeValue).toList();
    }

}
