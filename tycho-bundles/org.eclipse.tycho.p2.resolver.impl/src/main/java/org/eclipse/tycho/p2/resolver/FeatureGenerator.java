/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureManifestParser;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MavenModelFacade;
import org.eclipse.tycho.core.shared.MavenModelFacade.MavenLicense;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class FeatureGenerator {

    private static final String ELEMENT_DESCRIPTION = "description";
    private static final String FEATURE_XML_ENTRY = "feature.xml";
    private static final String ELEMENT_FEATURE = "feature";
    private static final String ELEMENT_PLUGIN = "plugin";
    private static final String ATTR_UNPACK = "unpack";
    private static final String ATTR_FRAGMENT = "fragment";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_URL = "url";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";

    public static Feature createFeatureFromTemplate(Element featureTemplate, List<IInstallableUnit> bundles,
            boolean isSourceFeature, MavenLogger logger)
            throws IOException, ParserConfigurationException, TransformerException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);
        Element featureElement = (Element) doc.importNode(featureTemplate, true);
        doc.appendChild(featureElement);
        return createFeature(featureElement, bundles, isSourceFeature, doc, logger);
    }

    private static Feature createFeature(Element featureElement, List<IInstallableUnit> bundles,
            boolean isSourceFeature, Document doc, MavenLogger logger) throws IOException, TransformerException,
            FileNotFoundException, StreamCorruptedException, SAXException, MalformedURLException {
        if (isSourceFeature) {
            featureElement.setAttribute(ATTR_ID, featureElement.getAttribute(ATTR_ID) + ".source");
            String nameAttribute = featureElement.getAttribute(ATTR_NAME);
            if (!nameAttribute.isBlank()) {
                featureElement.setAttribute(ATTR_NAME, nameAttribute + " (Source)");
            }
        }
        Set<IVersionedId> versionedIds = new HashSet<>();
        for (IInstallableUnit bundle : bundles) {
            //TODO can a feature contain the same id in different versions? PDE Editor seems not to support this...
            if (versionedIds.add(new VersionedId(bundle.getId(), bundle.getVersion()))) {
                boolean isFragment = bundle.getProvidedCapabilities().stream().anyMatch(
                        capability -> capability.getNamespace().equals(BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT));
                Element pluginElement = doc.createElement(ELEMENT_PLUGIN);
                pluginElement.setAttribute(ATTR_ID, bundle.getId());
                pluginElement.setAttribute(ATTR_VERSION, bundle.getVersion().toString());
                if (isFragment) {
                    pluginElement.setAttribute(ATTR_FRAGMENT, String.valueOf(true));
                }
                //TODO can we check form the IU if we need to unpack? Or is the bundle info required here? Does it actually matter at all?
                pluginElement.setAttribute(ATTR_UNPACK, String.valueOf(false));
                featureElement.appendChild(pluginElement);
            }
        }
        File tempFile = File.createTempFile("feature", ".jar");
        tempFile.deleteOnExit();
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(tempFile))) {
            stream.putNextEntry(new ZipEntry(FEATURE_XML_ENTRY));
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            prettyPrintXml(doc, writer);
            writer.flush();
            stream.closeEntry();
        }
        if (logger != null && logger.isDebugEnabled()) {
            StringWriter stringWriter = new StringWriter();
            prettyPrintXml(doc, stringWriter);
            logger.debug(stringWriter.toString());
        }
        try (JarFile jar = new JarFile(tempFile)) {
            JarEntry entry = jar.getJarEntry(FEATURE_XML_ENTRY);
            if (entry == null) {
                throw new StreamCorruptedException();
            }
            Feature feature = new FeatureManifestParser().parse(jar.getInputStream(entry),
                    tempFile.getAbsoluteFile().toURI().toURL());
            feature.setLocation(tempFile.getAbsolutePath());
            return feature;
        }
    }

    public static Feature generatePomFeature(MavenModelFacade model, List<IInstallableUnit> bundles,
            boolean isSourceFeature, MavenLogger logger)
            throws IOException, ParserConfigurationException, TransformerException, SAXException {
        String id = model.getGroupId() + "." + model.getArtifactId() + "." + model.getPackaging();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);
        Element featureElement = doc.createElement(ELEMENT_FEATURE);
        doc.appendChild(featureElement);
        featureElement.setAttribute(ATTR_ID, id);
        featureElement.setAttribute(ATTR_VERSION,
                WrappedArtifact.createOSGiVersionFromMaven(model.getVersion()).toString());
        String name = model.getName();
        if (isPresent(name)) {
            featureElement.setAttribute(ATTR_NAME, name);
        }
        if (isPresent(model.getDescription()) || isPresent(model.getUrl())) {
            Element descriptionElement = doc.createElement(ELEMENT_DESCRIPTION);
            descriptionElement.setAttribute(ATTR_URL, Objects.requireNonNullElse(model.getUrl(), ""));
            descriptionElement.insertBefore(doc.createTextNode(Objects.requireNonNullElse(model.getDescription(), "")),
                    descriptionElement.getLastChild());
            featureElement.appendChild(descriptionElement);
        }
        List<MavenLicense> licenses = model.getLicenses().collect(Collectors.toList());

        if (licenses.size() > 0) {
            Element licenseElement = doc.createElement("license");
            if (licenses.size() == 1) {
                MavenLicense license = licenses.get(0);
                licenseElement.setAttribute(ATTR_URL, Objects.requireNonNullElse(license.getUrl(), ""));
                licenseElement.insertBefore(doc.createTextNode(Objects.requireNonNullElse(license.getComments(), "")),
                        licenseElement.getLastChild());
            } else {
                licenseElement.setAttribute(ATTR_URL, "");
                String licenseInfo = licenses.stream().map(license -> Stream.<String> builder()//
                        .add(license.getName())//
                        .add(license.getUrl()).add(license.getComments())//
                        .build().filter(Objects::nonNull).filter(Predicate.not(String::isBlank))
                        .collect(Collectors.joining("\r\n")))
                        .collect(Collectors.joining("--------------------------------------------------\r\n"));
                licenseElement.insertBefore(doc.createTextNode(Objects.requireNonNullElse(licenseInfo, "")),
                        licenseElement.getLastChild());

            }
            featureElement.appendChild(licenseElement);
        }
        return createFeature(featureElement, bundles, isSourceFeature, doc, logger);
    }

    private static boolean isPresent(String label) {
        return label != null && !label.isBlank();
    }

    private static final void prettyPrintXml(Document xml, Writer writer) throws TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.transform(new DOMSource(xml), new StreamResult(writer));
    }
}
