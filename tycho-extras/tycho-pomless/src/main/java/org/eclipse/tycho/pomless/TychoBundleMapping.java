/*******************************************************************************
 * Copyright (c) 2019, 2023 Lablicate GmbH and others.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
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
    private static final String PDE_BND = "pde.bnd";

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    public float getPriority() {
        return 40;
    }

    @Override
    protected boolean isValidLocation(Path polyglotFile) {
        String fileName = getFileName(polyglotFile);
        if (fileName.equals(META_INF_DIRECTORY) && Files.isRegularFile(polyglotFile.resolve(MANIFEST_MF))) {
            return true;
        }
        return PDE_BND.equals(fileName) && Files.isRegularFile(polyglotFile);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File metaInfDirectory = new File(dir, META_INF_DIRECTORY);
        if (new File(metaInfDirectory, MANIFEST_MF).isFile()) {
            return metaInfDirectory;
        }
        File file = new File(dir, PDE_BND);
        if (file.isFile()) {
            return file;
        }
        return null;
    }

    @Override
    protected void initModel(Model model, Reader artifactReader, Path artifactFile) throws IOException {
        Path bundleRoot = artifactFile.getParent();
        Path manifestFile = getManifestFile(artifactFile);
        Attributes manifestHeaders = readManifestHeaders(manifestFile);
        String bundleSymbolicName = getBundleSymbolicName(manifestHeaders, manifestFile);
        // groupId is inherited from parent pom
        model.setArtifactId(bundleSymbolicName);
        String bundleVersion = getRequiredHeaderValue("Bundle-Version", manifestHeaders, manifestFile);
        model.setVersion(getPomVersion(bundleVersion));
        String prefix;
        if (isTestBundle(bundleSymbolicName, bundleRoot)) {
            model.setPackaging(PACKAGING_TEST);
            prefix = NAME_PREFIX_TEST;
        } else {
            prefix = NAME_PREFIX;
        }
        Path l10nFile = getBundleLocalizationPropertiesFile(manifestHeaders, manifestFile);
        Supplier<Properties> properties = getPropertiesSupplier(l10nFile);

        String bundleName = getManifestAttributeValue(manifestHeaders, "Bundle-Name", properties);
        model.setName(prefix + (bundleName != null ? bundleName : bundleSymbolicName));

        String vendorName = getManifestAttributeValue(manifestHeaders, "Bundle-Vendor", properties);
        if (vendorName != null) {
            Organization organization = new Organization();
            organization.setName(vendorName);
            model.setOrganization(organization);
        }
        String description = getManifestAttributeValue(manifestHeaders, "Bundle-Description", properties);
        if (description != null) {
            model.setDescription(description);
        }
        Path bndFile = bundleRoot.resolve("bnd.bnd");
        if (Files.isRegularFile(bndFile)) {
            createBndPlugin(model);
        }
    }

    private Path getManifestFile(Path artifactFile) {
        Path manifestFile = artifactFile.resolve(MANIFEST_MF);
        if (Files.isRegularFile(manifestFile)) {
            return manifestFile;
        }
        if (getFileName(artifactFile).equals(PDE_BND) && Files.isRegularFile(artifactFile)) {
            return artifactFile;
        }

        return manifestFile;
    }

    @Override
    protected Properties getEnhancementProperties(Path file) throws IOException {
        if (getFileName(file).equals(META_INF_DIRECTORY) && Files.isDirectory(file)) {
            //Look up build.properties in the project's root. The given file points to the 'META-INF' folder.
            return getBuildProperties(file.getParent());
        } else {
            if (getFileName(file).equals(PDE_BND)) {
                return loadProperties(file);
            }
            return super.getEnhancementProperties(file);
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
        process.setConfiguration(config);
        Xpp3Dom packagingTypes = new Xpp3Dom("packagingTypes");
        packagingTypes.setValue(model.getPackaging());
        config.addChild(packagingTypes);
        Xpp3Dom manifestPath = new Xpp3Dom("manifestPath");
        manifestPath.setValue("${project.build.directory}/BND.MF");
        config.addChild(manifestPath);
        return plugin;
    }

    private Attributes readManifestHeaders(Path manifestFile) throws IOException {
        Manifest manifest = new Manifest();
        try (InputStream stream = Files.newInputStream(manifestFile)) {
            if (getFileName(manifestFile).equals(PDE_BND)) {
                Properties properties = new Properties();
                properties.load(stream);
                Attributes attr = manifest.getMainAttributes();
                for (String key : properties.stringPropertyNames()) {
                    try {
                        //check if this is a valid manifest name...
                        new Name(key);
                    } catch (IllegalArgumentException e) {
                        // ... otherwise skip
                        continue;
                    }
                    attr.putValue(key, properties.getProperty(key));
                }
            } else {
                manifest.read(stream);
            }
        }
        return manifest.getMainAttributes();
    }

    private String getBundleSymbolicName(Attributes headers, Path manifestFile) throws ModelParseException {
        String symbolicName = getRequiredHeaderValue(BUNDLE_SYMBOLIC_NAME, headers, manifestFile);
        // strip off any directives/attributes
        int semicolonIndex = symbolicName.indexOf(';');
        if (semicolonIndex > 0) {
            symbolicName = symbolicName.substring(0, semicolonIndex);
        }
        return symbolicName;
    }

    private String getRequiredHeaderValue(String headerKey, Attributes headers, Path manifestFile)
            throws ModelParseException {
        String value = headers.getValue(headerKey);
        if (value == null) {
            throw new ModelParseException("Required header " + headerKey + " missing in " + manifestFile, -1, -1);
        }
        return value;
    }

    private boolean isTestBundle(String bundleSymbolicName, Path bundleRoot) throws IOException {
        Properties buildProperties = getBuildProperties(bundleRoot);
        // Although user defined packaging-type takes precedence, check it to have a corresponding artifactId
        String packagingProperty = buildProperties.getProperty(PACKAGING_PROPERTY);
        if (PACKAGING.equalsIgnoreCase(packagingProperty)) {
            return false;
        } else if (PACKAGING_TEST.equalsIgnoreCase(packagingProperty)) {
            return true;
        }
        return bundleSymbolicName.endsWith(".tests") || bundleSymbolicName.endsWith(".test");
        //TODO can we improve this? maybe also if the import/require bundle contains junit we should assume it's a test bundle?
        // or should we search for Test classes annotations?
    }

    private static String getManifestAttributeValue(Attributes headers, String attributeName,
            Supplier<Properties> localizationProperties) {
        String rawValue = headers.getValue(attributeName);

        String localizedValue = localizedValue(rawValue, localizationProperties);
        return localizedValue != null && !localizedValue.isBlank() ? localizedValue : null;
    }

    private static Path getBundleLocalizationPropertiesFile(Attributes headers, Path manifestFile) {
        String location = headers.getValue("Bundle-Localization");
        if (location == null || location.isEmpty()) {
            location = "OSGI-INF/l10n/bundle";
        }
        if (getFileName(manifestFile).equals(PDE_BND)) {
            return manifestFile.getParent().resolve(location + ".properties");
        }
        //we always use the default here to have consistent build regardless of locale settings
        return manifestFile.getParent().getParent().resolve(location + ".properties");
    }

}
