/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation derived  from TychoModelReader 
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

/**
 * 
 * Responsible for parsing eclipse-repository artifacts (updatesites, catgory.xml)
 */
@Component(role = Mapping.class, hint = TychoRepositoryMapping.PACKAGING)
public class TychoRepositoryMapping extends AbstractXMLTychoMapping {
    private static final String CATEGORY_XML = "category.xml";
    private static final String PRODUCT_EXTENSION = ".product";
    public static final String PACKAGING = "eclipse-repository";
    private static final String UPDATE_SITE_SUFFIX = "." + PACKAGING;

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException {
        if (artifactFile.getName().endsWith(PRODUCT_EXTENSION)) {
            initFromProdcut(model, xml);
        } else {
            initFromCategory(model, xml, artifactFile);
        }
    }

    private void initFromCategory(Model model, Element xml, File categoryXml) {
        String name = categoryXml.getParentFile().getName();
        if (!name.endsWith(UPDATE_SITE_SUFFIX)) {
            name = name + UPDATE_SITE_SUFFIX;
        }
        model.setArtifactId(name);
        model.setName(name);
    }

    private void initFromProdcut(Model model, Element xml) throws ModelParseException {
        model.setArtifactId(getRequiredXMLAttributeValue(xml, "uid"));
        String version = getXMLAttributeValue(xml, "version");
        if (version != null) {
            model.setVersion(getPomVersion(version));
        }
        String name = getXMLAttributeValue(xml, "name");
        if (name != null) {
            model.setName(name);
        }

        Build build = new Build();
        Plugin plugin = new Plugin();
        build.addPlugin(plugin);
        plugin.setArtifactId("tycho-p2-director-plugin");
        plugin.setGroupId("org.eclipse.tycho");

        PluginExecution materialize = new PluginExecution();
        materialize.setId("materialize-prodcuts");
        materialize.setGoals(Arrays.asList("materialize-products"));
        plugin.addExecution(materialize);
        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom products = new Xpp3Dom("products");
        Xpp3Dom product = new Xpp3Dom("product");
        Xpp3Dom id = new Xpp3Dom("id");
        id.setValue(model.getArtifactId());
        config.addChild(products);
        products.addChild(product);
        product.addChild(id);
        materialize.setConfiguration(config);
        PluginExecution archive = new PluginExecution();
        archive.setId("archive-prodcuts");
        archive.setGoals(Arrays.asList("archive-products"));
        archive.setConfiguration(config);
        plugin.addExecution(archive);

        model.setBuild(build);

    }

    @Override
    protected boolean isValidLocation(String location) {
        return location.endsWith(PRODUCT_EXTENSION) || location.endsWith(CATEGORY_XML);
    }

    @Override
    protected File getPrimaryArtifact(File projectRoot) {
        File[] productFiles = projectRoot.listFiles(
                (File dir, String name) -> name.endsWith(PRODUCT_EXTENSION) && !name.startsWith(".polyglot"));
        if (productFiles != null && productFiles.length > 0) {
            for (File file : productFiles) {
                if (file.isFile()) {
                    return file;
                }
            }
        }
        File category = new File(projectRoot, CATEGORY_XML);
        if (category.exists()) {
            return category;
        }
        return null;
    }

}
