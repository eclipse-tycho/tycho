/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.maven.polyglot.PolyglotModelManager;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Tycho POM model reader. Deduces maven model artifactId and version from OSGi manifest
 * Bundle-SymbolicName and Bundle-Version headers or feature.xml id and version attributes. Assumes
 * parent pom is located in parent directory (from which groupId is inherited). Bundles with
 * Bundle-SymbolicName ending with ".tests" will be assigned packaging type "eclipse-test-plugin".
 */
@Component(role = ModelReader.class, hint = "tycho")
public class TychoModelReader extends ModelReaderSupport {

    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String QUALIFIER_SUFFIX = ".qualifier";

    @Requirement
    private PolyglotModelManager polyglotModelManager;

    public TychoModelReader() {
    }

    public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        File projectRoot = new File(PolyglotModelUtil.getLocation(options)).getParentFile();
        File manifestFile = new File(projectRoot, "META-INF/MANIFEST.MF");
        File featureXml = new File(projectRoot, "feature.xml");
        if (manifestFile.isFile()) {
            return createPomFromManifest(manifestFile);
        } else if (featureXml.isFile()) {
            return createPomFromFeatureXml(featureXml);
        } else {
            throw new IOException("Neither META-INF/MANIFEST.MF nor feature.xml found in " + projectRoot);
        }
    }

    private Model createPomFromManifest(File manifestFile) throws IOException, ModelParseException {
        Attributes headers = readManifestHeaders(manifestFile);
        String bundleSymbolicName = getBundleSymbolicName(headers, manifestFile);
        Model model = createModel();
        model.setParent(findParent(manifestFile.getParentFile().getParentFile()));
        // groupId is inherited from parent pom
        model.setArtifactId(bundleSymbolicName);
        String bundleVersion = getRequiredHeaderValue("Bundle-Version", headers, manifestFile);
        model.setVersion(getPomVersion(bundleVersion));
        model.setPackaging(getPackagingType(bundleSymbolicName));
        return model;
    }

    private Model createPomFromFeatureXml(File featureXml) throws IOException, ModelParseException {
        Document doc;
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = parser.parse(featureXml);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new ModelParseException(e.getMessage(), -1, -1);
        }
        Element root = doc.getDocumentElement();
        Model model = createModel();
        model.setParent(findParent(featureXml.getParentFile()));
        Attr featureIdNode = root.getAttributeNode("id");
        if (featureIdNode == null) {
            throw new ModelParseException("missing feature id in " + featureXml.getAbsolutePath(), -1, -1);
        }
        model.setArtifactId(featureIdNode.getValue());
        Attr featureVersionNode = root.getAttributeNode("version");
        if (featureVersionNode == null) {
            throw new ModelParseException("missing feature version in " + featureXml.getAbsolutePath(), -1, -1);
        }
        model.setVersion(getPomVersion(featureVersionNode.getValue()));
        model.setPackaging("eclipse-feature");
        // groupId is inherited from parent pom
        return model;
    }

    private Model createModel() {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        return model;
    }

    private String getBundleSymbolicName(Attributes headers, File manifestFile) throws ModelParseException {
        String symbolicName = getRequiredHeaderValue(BUNDLE_SYMBOLIC_NAME, headers, manifestFile);
        // strip off any directives/attributes
        int semicolonIndex = symbolicName.indexOf(';');
        if (semicolonIndex > 0) {
            symbolicName = symbolicName.substring(0, semicolonIndex);
        }
        return symbolicName;
    }

    private String getRequiredHeaderValue(String headerKey, Attributes headers, File manifestFile)
            throws ModelParseException {
        String value = headers.getValue(headerKey);
        if (value == null) {
            throw new ModelParseException("Required header " + headerKey + " missing in " + manifestFile, -1, -1);
        }
        return value;
    }

    private Attributes readManifestHeaders(File manifestFile) throws IOException {
        Manifest manifest = new Manifest();
        FileInputStream stream = new FileInputStream(manifestFile);
        try {
            manifest.read(stream);
        } finally {
            stream.close();
        }
        return manifest.getMainAttributes();
    }

    private static String getPomVersion(String pdeVersion) {
        String pomVersion = pdeVersion;
        if (pdeVersion.endsWith(QUALIFIER_SUFFIX)) {
            pomVersion = pdeVersion.substring(0, pdeVersion.length() - QUALIFIER_SUFFIX.length()) + "-SNAPSHOT";
        }
        return pomVersion;
    }

    private String getPackagingType(String symbolicName) {
        // assume test bundles end with ".tests"
        if (symbolicName.endsWith(".tests")) {
            return "eclipse-test-plugin";
        } else {
            return "eclipse-plugin";
        }
    }

    Parent findParent(File projectRoot) throws ModelParseException, IOException {
        // assumption/limitation: parent pom must be physically located in
        // parent directory
        File parentPom = polyglotModelManager.locatePom(projectRoot.getParentFile());
        if (parentPom == null) {
            throw new FileNotFoundException("No parent pom file found in " + projectRoot.getParentFile());
        }
        Map<String, File> options = new HashMap<String, File>(4);
        options.put(ModelProcessor.SOURCE, parentPom);
        ModelReader reader = polyglotModelManager.getReaderFor(options);
        Model parentModel = reader.read(parentPom, options);
        Parent parentReference = new Parent();
        String groupId = parentModel.getGroupId();
        if (groupId == null) {
            // must be inherited from grandparent
            groupId = parentModel.getParent().getGroupId();
        }
        parentReference.setGroupId(groupId);
        parentReference.setArtifactId(parentModel.getArtifactId());
        String version = parentModel.getVersion();
        if (version == null) {
            // must be inherited from grandparent
            version = parentModel.getParent().getVersion();
        }
        parentReference.setVersion(version);
        return parentReference;
    }
}
