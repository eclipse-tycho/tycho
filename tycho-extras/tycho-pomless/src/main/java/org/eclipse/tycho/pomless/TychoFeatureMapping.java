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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

@Named(TychoFeatureMapping.PACKAGING)
@Singleton
public class TychoFeatureMapping extends AbstractXMLTychoMapping {

    private static final String NAME_PREFIX = "[feature] ";
    private static final String FEATURE_XML = "feature.xml";
    public static final String PACKAGING = "eclipse-feature";

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    public float getPriority() {
        return 30;
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, Path artifactFile) throws IOException {
        model.setArtifactId(getRequiredXMLAttributeValue(xml, "id"));
        model.setVersion(getPomVersion(getRequiredXMLAttributeValue(xml, "version"), model, artifactFile));

        Path featureProperties = artifactFile.getParent().resolve("feature.properties");
        Supplier<Properties> properties = getPropertiesSupplier(featureProperties);

        String label = localizedValue(getXMLAttributeValue(xml, "label"), properties);
        model.setName(NAME_PREFIX + (label != null ? label : model.getArtifactId()));

        String provider = localizedValue(getXMLAttributeValue(xml, "provider-name"), properties);
        if (provider != null) {
            Organization organization = new Organization();
            organization.setName(provider);
            model.setOrganization(organization);
        }
    }

    @Override
    protected boolean isValidLocation(Path location) {
        return getFileName(location).equals(FEATURE_XML);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File featureXml = new File(dir, FEATURE_XML);
        if (featureXml.exists()) {
            return featureXml;
        }
        return null;
    }

}
