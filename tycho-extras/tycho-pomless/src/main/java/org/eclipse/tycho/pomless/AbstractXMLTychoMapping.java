/*******************************************************************************
 * Copyright (c) 2019, 2020 Lablicate GmbH and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Christoph Läubrich (Lablicate GmbH) - initial API and implementation
 * Christoph Läubrich -     Bug 562887 - Support multiple product files
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    protected void initModel(Model model, Reader artifactReader, Path artifactFile) throws IOException {
        initModelFromXML(model, parseXML(artifactReader, artifactFile.toUri().toASCIIString()), artifactFile);
    }

    protected abstract void initModelFromXML(Model model, Element xml, Path artifactFile) throws IOException;

    protected static Element parseXML(Reader artifactReader, String documentURI) throws IOException {
        try {
            DocumentBuilder parser = FACTORY.newDocumentBuilder();
            Document doc = parser.parse(new InputSource(artifactReader));
            if (documentURI != null) {
                doc.setDocumentURI(documentURI);
            }
            return doc.getDocumentElement();

        } catch (ParserConfigurationException e) {
            throw new IOException("parser failed", e);
        } catch (SAXException e) {
            int lineNumber = -1;
            int columnNumber = -1;
            if (e instanceof SAXParseException parseException) {
                lineNumber = parseException.getLineNumber();
                columnNumber = parseException.getColumnNumber();
            }
            throw new ModelParseException(e.getMessage(), lineNumber, columnNumber, e);
        }
    }

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

    @Override
    public float getPriority() {
        return 1;
    }

    protected static Stream<File> filesWithExtension(Path directory, String extension) throws IOException {
        Predicate<String> nameFilter = n -> !n.startsWith(".polyglot.") && n.endsWith(extension);
        return Files.walk(directory, 1) //
                .filter(p -> nameFilter.test(getFileName(p))) //
                .filter(Files::isRegularFile).map(Path::toFile);
    }
}
