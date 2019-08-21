/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation derived from TychoModelReader 
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

@Component(role = Mapping.class, hint = TychoFeatureMapping.PACKAGING)
public class TychoFeatureMapping extends AbstractXMLTychoMapping {

    private static final String FEATURE_XML = "feature.xml";
    public static final String PACKAGING = "eclipse-feature";

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException {
        model.setArtifactId(getRequiredXMLAttributeValue(xml, "id"));
        model.setVersion(getPomVersion(getRequiredXMLAttributeValue(xml, "version")));
        String label = getXMLAttributeValue(xml, "label");
        if (label != null) {
            model.setName(label);
        }
        String provider = getXMLAttributeValue(xml, "provider-name");
        if (provider != null) {
            Organization organization = new Organization();
            organization.setName(provider);
            model.setOrganization(organization);
        }
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
