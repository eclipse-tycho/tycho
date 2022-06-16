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
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Component(role = Mapping.class, hint = TychoBundleMapping.PACKAGING)
public class TychoBundleMapping extends AbstractTychoMapping {

    private static final String NAME_PREFIX = "[bundle] ";
    private static final String NAME_PREFIX_TEST = "[test-bundle] ";
    public static final String META_INF_DIRECTORY = "META-INF";
    public static final String MANIFEST_MF = "MANIFEST.MF";
    public static final String PACKAGING = "eclipse-plugin";

    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String PACKAGING_TEST = "eclipse-test-plugin";

    @Override
    protected boolean isValidLocation(String location) {
        File polyglotArtifactFile = new File(location);
        if (polyglotArtifactFile.isDirectory() && polyglotArtifactFile.getName().equals(META_INF_DIRECTORY)) {
            return new File(polyglotArtifactFile, MANIFEST_MF).exists();
        }
        return false;
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File metaInfDirectory = new File(dir, META_INF_DIRECTORY);
        if (metaInfDirectory.isDirectory()) {
            File manifestFile = new File(metaInfDirectory, MANIFEST_MF);
            if (manifestFile.isFile()) {
                return metaInfDirectory;
            }
        }
        return null;
    }

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    protected void initModel(Model model, Reader artifactReader, File artifactFile)
            throws ModelParseException, IOException {
        File bundleRoot = artifactFile.getParentFile();
        File manifestFile = new File(artifactFile, MANIFEST_MF);
        Attributes manifestHeaders = readManifestHeaders(manifestFile);
        String bundleSymbolicName = getBundleSymbolicName(manifestHeaders, manifestFile);
        // groupId is inherited from parent pom
        model.setArtifactId(bundleSymbolicName);
        String bundleVersion = getRequiredHeaderValue("Bundle-Version", manifestHeaders, manifestFile);
        model.setVersion(getPomVersion(bundleVersion));
        String prefix;
        if (isTestBundle(bundleSymbolicName, manifestHeaders, bundleRoot)) {
            model.setPackaging(PACKAGING_TEST);
            prefix = NAME_PREFIX_TEST;
        } else {
            prefix = NAME_PREFIX;
        }
        String bundleName = getManifestAttributeValue(manifestHeaders, "Bundle-Name", manifestFile);
        if (bundleName != null) {
            model.setName(prefix + bundleName);
        } else {
            model.setName(prefix + bundleSymbolicName);
        }
        String vendorName = getManifestAttributeValue(manifestHeaders, "Bundle-Vendor", manifestFile);
        if (vendorName != null) {
            Organization organization = new Organization();
            organization.setName(vendorName);
            model.setOrganization(organization);
        }
        String description = getManifestAttributeValue(manifestHeaders, "Bundle-Description", manifestFile);
        if (description != null) {
            model.setDescription(description);
        }
        File bndFile = new File(bundleRoot, "bnd.bnd");
        if (bndFile.exists()) {
            createBndPlugin(model);
        }

    }

    private static Plugin createBndPlugin(Model model) {
        //See https://github.com/bndtools/bnd/blob/master/maven/bnd-maven-plugin/README.md#bnd-process-goal 
        Build build = model.getBuild();
        if (build == null) {
            model.setBuild(build = new Build());
        }
        Plugin plugin = new Plugin();
        plugin.setGroupId("biz.aQute.bnd");
        plugin.setArtifactId("bnd-maven-plugin");
        build.addPlugin(plugin);
        PluginExecution process = new PluginExecution();
        process.setId("bnd-process");
        process.setGoals(Arrays.asList("bnd-process"));
        plugin.addExecution(process);
        Xpp3Dom config = new Xpp3Dom("configuration");
        process.setConfiguration(config = new Xpp3Dom("configuration"));
        Xpp3Dom packagingTypes = new Xpp3Dom("packagingTypes");
        packagingTypes.setValue(model.getPackaging());
        config.addChild(packagingTypes);
        Xpp3Dom manifestPath = new Xpp3Dom("manifestPath");
        manifestPath.setValue("${project.build.directory}/BND.MF");
        config.addChild(manifestPath);
        return plugin;
    }

    private Attributes readManifestHeaders(File manifestFile) throws IOException {
        Manifest manifest = new Manifest();
        try (FileInputStream stream = new FileInputStream(manifestFile)) {
            manifest.read(stream);
        }
        return manifest.getMainAttributes();
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

    private boolean isTestBundle(String bundleSymbolicName, Attributes manifestHeaders, File bundleRoot)
            throws IOException {
        Properties buildProperties = getBuildProperties(bundleRoot);
        String property = buildProperties.getProperty("tycho.pomless.testbundle");
        if (property != null) {
            //if property is given it take precedence over our guesses...
            return Boolean.valueOf(property);
        }
        if (bundleSymbolicName.endsWith(".tests") || bundleSymbolicName.endsWith(".test")) {
            //TODO can we improve this? maybe also if the import/require bundle contains junit we should assume its a test bundle?
            // or should we search for Test classes annotations? 
            return true;
        }
        return false;
    }

    private static String getManifestAttributeValue(Attributes headers, String attributeName, File manifestFile)
            throws IOException {
        String location = headers.getValue("Bundle-Localization");
        if (location == null || location.isEmpty()) {
            location = "OSGI-INF/l10n/bundle";
        }
        String rawValue = headers.getValue(attributeName);
        if (rawValue != null && !rawValue.isEmpty()) {
            if (rawValue.startsWith("%")) {
                String key = rawValue.substring(1);
                //we always use the default here to have consistent build regardless of locale settings
                File l10nFile = new File(manifestFile.getParentFile().getParentFile(), location + ".properties");
                if (l10nFile.exists()) {
                    Properties properties = new Properties();
                    try (InputStream stream = new FileInputStream(l10nFile)) {
                        properties.load(stream);
                    }
                    String translation = properties.getProperty(key);
                    if (translation != null && !translation.isEmpty()) {
                        return translation;
                    }
                }
                return key;
            }
            return rawValue;
        }
        return null;
    }

}
