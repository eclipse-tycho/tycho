/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
package org.eclipse.tycho.test.product;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

/**
 * @deprecated use {@link org.eclipse.tycho.test.util.P2RepositoryTool} instead
 */
@Deprecated
class Util {
    public static Document openXmlFromZip(File zipFile, String xmlFile) throws IOException, ZipException {
        XMLParser parser = new XMLParser();
        ZipFile zip = new ZipFile(zipFile);
        try {
            ZipEntry contentXmlEntry = zip.getEntry(xmlFile);
            InputStream entryStream = zip.getInputStream(contentXmlEntry);
            return parser.parse(new XMLIOSource(entryStream));
        } finally {
            zip.close();
        }
    }

    static public boolean containsIUWithProperty(Document contentXML, String iuId, String propName, String propValue) {
        Set<Element> ius = findIU(contentXML, iuId);
        for (Element unitElement : ius) {
            if (iuHasProperty(unitElement, propName, propValue))
                return true;
        }
        return false;
    }

    static public Set<Element> findIU(Document contentXML, String iuId) {
        Set<Element> foundIUs = new HashSet<>();

        Element repository = contentXML.getRootElement();
        for (Element unit : repository.getChild("units").getChildren("unit")) {
            if (iuId.equals(unit.getAttributeValue("id"))) {
                foundIUs.add(unit);
            }
        }
        return foundIUs;
    }

    static public boolean iuHasProperty(Element unit, String propName, String propValue) {
        boolean foundIU = false;

        if (propName != null) {
            for (Element property : unit.getChild("properties").getChildren("property")) {
                if (propName.equals(property.getAttributeValue("name"))
                        && propValue.equals((property.getAttributeValue("value")))) {
                    foundIU = true;
                    break;
                }
            }
        } else {
            foundIU = true;
        }
        return foundIU;
    }

    static public boolean iuHasAllRequirements(Element unit, String... requiredIus) {
        boolean hasAllRequirements = true;
        for (String requiredIu : requiredIus) {
            boolean foundIU = false;
            for (Element property : unit.getChild("requires").getChildren("required")) {
                if (requiredIu.equals(property.getAttributeValue("name"))) {
                    foundIU = true;
                    break;
                }
            }
            if (!foundIU) {
                hasAllRequirements = false;
                break;
            }
        }
        return hasAllRequirements;
    }

    static public boolean iuHasTouchpointDataInstruction(Element unit, String instructionTrimmedText) {
        Element touchpointDataElem = unit.getChild("touchpointData");

        if (touchpointDataElem != null) {
            for (Element instructions : touchpointDataElem.getChildren("instructions")) {
                for (Element instruction : instructions.getChildren("instruction")) {
                    if (instructionTrimmedText.equals(instruction.getTrimmedText())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
