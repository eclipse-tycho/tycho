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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final List<String> PRODUCT_EXECUTIONS = List.of(ARCHIVE_PRODUCTS_ID, MATERIALIZE_PRODUCTS_ID);
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
    protected void initModel(Model model, Reader artifactReader, File artifactFile) throws IOException {
        if (artifactFile.getName().endsWith(PRODUCT_EXTENSION)) {
            File projectRoot = artifactFile.getParentFile();
            try (var files = filesWithExtension(projectRoot.toPath(), PRODUCT_EXTENSION)) {
                List<File> products = files.collect(Collectors.toList());
                if (products.size() > 1) {
                    //multiple products must inherit version from parent but get the artifact-id from the parent-folder
                    model.setArtifactId(projectRoot.getName());
                    Plugin directorPlugin = createDirectorPlugin(model);
                    List<String> names = new ArrayList<>();
                    for (File file : products) {
                        Element productXml;
                        try (FileReader reader = new FileReader(file, getPrimaryArtifactCharset())) {
                            productXml = parseXML(reader, file.toURI().toASCIIString());
                        }
                        String baseName = FilenameUtils.getBaseName(file.getName());
                        String name = getXMLAttributeValue(productXml, PRODUCT_NAME_ATTRIBUTE);
                        names.add(null == name ? baseName : name);
                        addProduct(directorPlugin, productXml, baseName);
                    }
                    model.setName(PRODUCT_NAME_PREFIX + String.join(", ", names));
                    return;
                }
            }
        }
        super.initModel(model, artifactReader, artifactFile);
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile) throws IOException {
        if (artifactFile.getName().endsWith(PRODUCT_EXTENSION)) {
            model.setArtifactId(getRequiredXMLAttributeValue(xml, PRODUCT_UID_ATTRIBUTE));
            String version = getXMLAttributeValue(xml, PRODUCT_VERSION_ATTRIBUTE);
            if (version != null) {
                model.setVersion(getPomVersion(version));
            }
            String name = getXMLAttributeValue(xml, PRODUCT_NAME_ATTRIBUTE);
            model.setName(PRODUCT_NAME_PREFIX + (name != null ? name : model.getArtifactId()));
            addProduct(createDirectorPlugin(model), xml, null);
        } else {
            initFromCategory(model, artifactFile);
        }
    }

    private static void initFromCategory(Model model, File categoryXml) {
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
        Optional<File> repoFile;
        try (var files = filesWithExtension(projectRoot.toPath(), PRODUCT_EXTENSION)) {
            repoFile = files.findFirst();
        } catch (IOException e) {
            repoFile = Optional.empty();// ignore
        }
        return repoFile.or(() -> Optional.of(new File(projectRoot, CATEGORY_XML)).filter(File::exists)).orElse(null);
    }

    private static void addProduct(Plugin directorPlugin, Element productXml, String attachId)
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

    private static Plugin createDirectorPlugin(Model model) {
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
}
