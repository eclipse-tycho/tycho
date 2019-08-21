/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * Baseclass for models derived from xml sources
 */
public abstract class AbstractXMLTychoMapping extends AbstractTychoMapping {

    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    @Override
    protected void initModel(Model model, Reader artifactReader, File artifactFile)
            throws ModelParseException, IOException {
        try {
            DocumentBuilder parser = FACTORY.newDocumentBuilder();
            Document doc = parser.parse(new InputSource(artifactReader));
            doc.setDocumentURI(artifactFile.toURI().toASCIIString());
            Element element = doc.getDocumentElement();
            initModelFromXML(model, element, artifactFile);
        } catch (ParserConfigurationException e) {
            throw new IOException("parser failed", e);
        } catch (SAXException e) {
            int lineNumber = -1;
            int columnNumber = -1;
            if (e instanceof SAXParseException) {
                SAXParseException parseException = (SAXParseException) e;
                lineNumber = parseException.getLineNumber();
                columnNumber = parseException.getColumnNumber();
            }
            throw new ModelParseException(e.getMessage(), lineNumber, columnNumber, e);
        }
    }

    protected abstract void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException;

    protected static String getRequiredXMLAttributeValue(Element element, String attributeName)
            throws ModelParseException {
        String value = getXMLAttributeValue(element, attributeName);
        if (value == null) {
            throw new ModelParseException(String.format("missing or empty '%s' attribute in element '%s' (uri=%s)",
                    attributeName, element.getNodeName(), element.getOwnerDocument().getDocumentURI()), -1, -1);
        }
        return value;
    }

    protected static String getXMLAttributeValue(Element element, String attributeName) {
        Attr idNode = element.getAttributeNode(attributeName);
        if (idNode != null) {
            String value = idNode.getValue();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
