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
 * Christoph Läubrich -     Bug 562887 - Support multiple product files
 * 
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
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
    private static final String ARCHIVE_PRODUCTS_ID = "archive-products";
    private static final String MATERIALIZE_PRODUCTS_ID = "materialize-products";
    private static final String[] PRODUCT_EXECUTIONS = { ARCHIVE_PRODUCTS_ID, MATERIALIZE_PRODUCTS_ID };
    private static final String PRODUCT_NAME_PREFIX = "[product] ";
    private static final String PRODUCT_NAME_ATTRIBUTE = "name";
    private static final String PRODUCT_VERSION_ATTRIBUTE = "version";
    private static final String PRODUCT_UID_ATTRIBUTE = "uid";
    private static final String CATEGORY_XML = "category.xml";
    private static final String PRODUCT_EXTENSION = ".product";
    public static final String PACKAGING = "eclipse-repository";
    private static final String UPDATE_SITE_SUFFIX = "." + PACKAGING;

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    protected void initModel(Model model, Reader artifactReader, File artifactFile)
            throws ModelParseException, IOException {
        if (artifactFile.getName().endsWith(PRODUCT_EXTENSION)) {
            File projectRoot = artifactFile.getParentFile();
            File[] products = listProducts(projectRoot);
            if (products.length > 1) {
                //multiple products must inherit version from parent but get the artifact-id from the parent-folder
                model.setArtifactId(projectRoot.getName());
                Plugin directorPlugin = createDirectorPlugin(model);
                List<String> names = new ArrayList<>();
                for (File file : products) {
                    Element productXml = parseXML(new FileReader(file, getPrimaryArtifactCharset()),
                            file.toURI().toASCIIString());
                    String baseName = FilenameUtils.getBaseName(file.getName());
                    String name = getXMLAttributeValue(productXml, PRODUCT_NAME_ATTRIBUTE);
                    if (name == null) {
                        names.add(baseName);
                    } else {
                        names.add(name);
                    }
                    addProduct(directorPlugin, productXml, baseName);
                }
                model.setName(PRODUCT_NAME_PREFIX + String.join(", ", names));
                return;
            }
        }
        super.initModel(model, artifactReader, artifactFile);
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException {
        if (artifactFile.getName().endsWith(PRODUCT_EXTENSION)) {
            model.setArtifactId(getRequiredXMLAttributeValue(xml, PRODUCT_UID_ATTRIBUTE));
            String version = getXMLAttributeValue(xml, PRODUCT_VERSION_ATTRIBUTE);
            if (version != null) {
                model.setVersion(getPomVersion(version));
            }
            String name = getXMLAttributeValue(xml, PRODUCT_NAME_ATTRIBUTE);
            if (name != null) {
                model.setName(PRODUCT_NAME_PREFIX + name);
            } else {
                model.setName(PRODUCT_NAME_PREFIX + model.getArtifactId());
            }
            addProduct(createDirectorPlugin(model), xml, null);
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
        model.setName("[updatesite] " + name);
    }

    @Override
    protected boolean isValidLocation(String location) {
        return location.endsWith(PRODUCT_EXTENSION) || location.endsWith(CATEGORY_XML);
    }

    @Override
    protected File getPrimaryArtifact(File projectRoot) {
        File[] productFiles = listProducts(projectRoot);
        if (productFiles.length > 0) {
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

    public static void addProduct(Plugin directorPlugin, Element productXml, String attachId)
            throws ModelParseException {
        Map<String, PluginExecution> map = directorPlugin.getExecutionsAsMap();
        for (String executionId : PRODUCT_EXECUTIONS) {
            PluginExecution pluginExecution = map.computeIfAbsent(executionId, required -> {
                throw new IllegalArgumentException(required + " PluginExecution is missing");
            });
            Xpp3Dom config = (Xpp3Dom) pluginExecution.getConfiguration();
            if (config == null) {
                pluginExecution.setConfiguration(config = new Xpp3Dom("configuration"));
            }
            Xpp3Dom products = config.getChild("products");
            if (products == null) {
                config.addChild(products = new Xpp3Dom("products"));
            }
            Xpp3Dom product = new Xpp3Dom("product");
            Xpp3Dom id = new Xpp3Dom("id");
            id.setValue(getRequiredXMLAttributeValue(productXml, PRODUCT_UID_ATTRIBUTE));
            product.addChild(id);
            if (attachId != null) {
                Xpp3Dom attach = new Xpp3Dom("attachId");
                attach.setValue(attachId);
                product.addChild(attach);
            }
            products.addChild(product);
        }
    }

    public static Plugin createDirectorPlugin(Model model) {
        Build build = model.getBuild();
        if (build == null) {
            model.setBuild(build = new Build());
        }
        Plugin plugin = new Plugin();
        plugin.setArtifactId("tycho-p2-director-plugin");
        plugin.setGroupId("org.eclipse.tycho");
        build.addPlugin(plugin);
        PluginExecution materialize = new PluginExecution();
        materialize.setId(MATERIALIZE_PRODUCTS_ID);
        materialize.setGoals(Arrays.asList(MATERIALIZE_PRODUCTS_ID));
        plugin.addExecution(materialize);
        PluginExecution archive = new PluginExecution();
        archive.setId(ARCHIVE_PRODUCTS_ID);
        archive.setGoals(Arrays.asList(ARCHIVE_PRODUCTS_ID));
        plugin.addExecution(archive);
        return plugin;
    }

    public static File[] listProducts(File projectRoot) {
        File[] productFiles = projectRoot.listFiles(
                (File dir, String name) -> name.endsWith(PRODUCT_EXTENSION) && !name.startsWith(".polyglot"));
        if (productFiles == null) {
            return new File[0];
        }
        return productFiles;
    }

}
