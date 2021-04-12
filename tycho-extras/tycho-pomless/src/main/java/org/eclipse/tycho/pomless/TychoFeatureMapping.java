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
 * Christoph Läubrich (Lablicate GmbH) - initial API and implementation derived from TychoModelReader
 * Christoph Läubrich - add type prefix to name 
 *  
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

@Component(role = Mapping.class, hint = TychoFeatureMapping.PACKAGING)
public class TychoFeatureMapping extends AbstractXMLTychoMapping {

    private static final String NAME_PREFIX = "[feature] ";
    private static final String FEATURE_XML = "feature.xml";
    public static final String PACKAGING = "eclipse-feature";

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException {
        model.setArtifactId(getRequiredXMLAttributeValue(xml, "id"));
        model.setVersion(getPomVersion(getRequiredXMLAttributeValue(xml, "version")));
        Properties featureProperties = new Properties();
        loadFeatureProperties(artifactFile, featureProperties);
        String label = getExternalizedXMLAtttributeValue(xml, featureProperties, "label");
        if (label != null) {
            model.setName(NAME_PREFIX + label);
        } else {
            model.setName(NAME_PREFIX + model.getArtifactId());
        }
        String provider = getExternalizedXMLAtttributeValue(xml, featureProperties, "provider-name");
        if (provider != null) {
            Organization organization = new Organization();
            organization.setName(provider);
            model.setOrganization(organization);
        }
    }

    private void loadFeatureProperties(File artifactFile, Properties externalized) {
        File featureProperties = new File(artifactFile.getParentFile(), "feature.properties");
        if (featureProperties.exists()) {
            try (InputStream stream = new FileInputStream(featureProperties)) {
                externalized.load(stream);
            } catch (IOException e) {
                // ignore externalzied data
            }
        }
    }

    private String getExternalizedXMLAtttributeValue(Element element, Properties properties, String attributeName) {
        String attribute = getXMLAttributeValue(element, attributeName);
        if (attribute != null && !attribute.isEmpty() && attribute.startsWith("%")) {
            //load value from feature properties
            String translation = properties.getProperty(attribute.substring(1));
            if (translation != null && !translation.isEmpty()) {
                return translation;
            }
        }
        return attribute;

    }

    @Override
    protected boolean isValidLocation(String location) {
        return location.endsWith(FEATURE_XML);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File featureXml = new File(dir, FEATURE_XML);
        if (featureXml.exists()) {
            return featureXml;
        }
        return null;
    }

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

}
