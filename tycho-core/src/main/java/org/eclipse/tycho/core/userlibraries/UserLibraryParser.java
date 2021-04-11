/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.userlibraries;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UserLibraryParser {

    public static Collection<UserLibrary> parse(File file) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            NodeList libraryElements = doc.getDocumentElement().getElementsByTagName("library");
            int libraryElementsLength = libraryElements.getLength();
            List<UserLibrary> list = new ArrayList<>();
            for (int i = 0; i < libraryElementsLength; i++) {
                Element libraryElement = (Element) libraryElements.item(i);
                String name = libraryElement.getAttribute("name");
                UserLibrary userLibrary = new UserLibrary(name);
                list.add(userLibrary);
                NodeList archiveElements = libraryElement.getElementsByTagName("archive");
                int archiveElementsLength = archiveElements.getLength();
                for (int j = 0; j < archiveElementsLength; j++) {
                    Element archiveElement = (Element) archiveElements.item(j);
                    userLibrary.addPath(archiveElement.getAttribute("path"));
                }
            }
            return list;
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

}
