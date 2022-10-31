/*******************************************************************************
 * Copyright (c) 2019, 2022 Lablicate GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Christoph Läubrich - initial API and implementation,
 *                      derived methods setLocation/findParent/getPomVersion from TychoModelReader
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.mapping.Mapping;

/**
 * Base implementation for a {@link Mapping} and {@link ModelReader} that handles all the low-level
 * stuff, implementations must only handle a small subset
 *
 */
public abstract class AbstractTychoMapping implements Mapping, ModelReader {

    // All build.properties entries specifically considered by Tycho. Extends the list in Mapping interface
    protected static final String TYCHO_POMLESS_PARENT_PROPERTY = "tycho.pomless.parent";
    protected static final String TYCHO_POMLESS_AGGREGATOR_NAMES_PROPERTY = "tycho.pomless.aggregator.names";

    private static final String PARENT_POM_DEFAULT_VALUE = System.getProperty(TYCHO_POMLESS_PARENT_PROPERTY, "..");
    private static final String QUALIFIER_SUFFIX = ".qualifier";
    private static final String MODEL_PARENT = "TychoMapping.model.parent";

    @Requirement
    protected PlexusContainer container;

    @Requirement
    protected Logger logger;

    private ModelWriter writer;
    private boolean extensionMode;
    @SuppressWarnings("unused")
    private File multiModuleProjectDirectory;
    private String snapshotFormat;

    @Override
    public File locatePom(File dir) {
        File file = new File(dir, "pom.xml");
        if (file.exists()) {
            //we want that pom.xml takes precedence over our generated one
            return null;
        }
        return getPrimaryArtifact(dir);
    }

    @Override
    public boolean accept(Map<String, ?> options) {
        Optional<Path> location = getLocation(options);
        if (location.isEmpty() || getFileName(location.get()).equals("pom.xml")) {
            //we want that pom.xml takes precedence over our generated one
            return false;
        }
        return isValidLocation(location.get());
    }

    @Override
    public ModelReader getReader() {
        return this;
    }

    @Override
    public ModelWriter getWriter() {
        if (writer == null) {
            try {
                assert container != null;
                writer = container.lookup(ModelWriter.class, getFlavour());
            } catch (ComponentLookupException e) {
                throw new RuntimeException(e);
            }
        }
        return writer;
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        Path file = getLocation(options).orElseThrow(() -> new IOException("Failed to obtain file location"));
        try (InputStreamReader stream = new InputStreamReader(input, getPrimaryArtifactCharset())) {
            return read(stream, file, options);
        }
    }

    @Override
    public Model read(File inputFile, Map<String, ?> options) throws IOException {
        Path input = inputFile.toPath();
        Path artifactFile = getRealArtifactFile(input);
        if (Files.isDirectory(artifactFile)) {
            return read(new StringReader(""), input, options);
        } else if (Files.isRegularFile(artifactFile)) {
            try (Reader stream = Files.newBufferedReader(artifactFile, getPrimaryArtifactCharset())) {
                return read(stream, input, options);
            }
        } else {
            //we support the case here, that actually there is no real primary artifact, instead of creating a dummy file and forcing I/O we simply simulate to read a 0-byte file
            return read(new StringReader(""), input, options);
        }
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        Path file = getLocation(options).orElseThrow(() -> new IOException("Failed to obtain file location"));
        return read(input, file, options);
    }

    private Model read(Reader artifactReader, Path artifactFile, Map<String, ?> options) throws IOException {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setPackaging(getPackaging());
        initModel(model, artifactReader, artifactFile);
        if (model.getParent() == null) {
            model.setParent(findParent(artifactFile.getParent(), options));
        }
        if (model.getVersion() == null) {
            //inherit version from parent if not given
            model.setVersion(model.getParent().getVersion());
        }
        if (model.getName() == null) {
            model.setName(model.getArtifactId());
        }
        setLocation(model, getRealArtifactFile(artifactFile));
        return model;
    }

    protected Path getRealArtifactFile(Path polyglotArtifactFile) {
        return polyglotArtifactFile;
    }

    protected Parent findParent(Path projectRoot, Map<String, ?> projectOptions) throws IOException {
        Parent parent = (Parent) projectOptions.get(MODEL_PARENT);
        if (parent != null) {
            //if the parent is given by the options we don't need to search it!
            return parent;
        }
        Properties buildProperties = getBuildProperties(projectRoot);
        // assumption parent pom must be physically located in parent directory if not given by build.properties
        String parentRef = buildProperties.getProperty(TYCHO_POMLESS_PARENT_PROPERTY, PARENT_POM_DEFAULT_VALUE);
        Path fileOrFolder = projectRoot.resolve(parentRef).toRealPath();
        PomReference parentPom;
        if (Files.isRegularFile(fileOrFolder)) {
            parentPom = locatePomReference(fileOrFolder.getParent(), getFileName(fileOrFolder));
        } else if (Files.isDirectory(fileOrFolder)) {
            parentPom = locatePomReference(fileOrFolder, null);
        } else {
            throw new FileNotFoundException("parent pom file/folder " + fileOrFolder + " is not accessible");
        }
        if (parentPom == null) {
            throw new FileNotFoundException("No parent pom file found in " + fileOrFolder.toRealPath());
        }
        Map<String, Object> options = new HashMap<>(1);
        options.put(ModelProcessor.SOURCE, new FileModelSource(parentPom.getPomFile()));
        Model parentModel = parentPom.getReader().read(parentPom.getPomFile(), options);
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
        parentReference
                .setRelativePath(projectRoot.toRealPath().relativize(parentPom.getPomFile().toPath()).toString());
        logger.debug("Derived parent for path " + projectRoot + " is groupId: " + parentReference.getGroupId()
                + ", artifactId: " + parentReference.getArtifactId() + ", relativePath: "
                + parentReference.getRelativePath());
        return parentReference;
    }

    /**
     * Locates the {@link PomReference} for the given folder and the given nameHint
     *
     * @param folder
     *            the folder to search
     * @param nameHint
     *            the name hint to use
     * @return the {@link PomReference} or <code>null</code>
     */
    protected PomReference locatePomReference(Path folderPath, String nameHint) {
        File folder = folderPath.toFile();
        PomReference reference = null;
        try {
            List<ModelProcessor> lookupList = container.lookupList(ModelProcessor.class);

            for (ModelProcessor processor : lookupList) {
                File pom = processor.locatePom(folder);
                if (pom != null && (reference == null || pom.getName().equals(nameHint)) && pom.exists()) {
                    reference = new PomReference(pom, processor);
                }
            }
        } catch (ComponentLookupException e) {
        }
        return reference;
    }

    @Override
    public String getFlavour() {
        return getPackaging();
    }

    protected abstract boolean isValidLocation(Path location);

    protected abstract File getPrimaryArtifact(File dir);

    protected abstract String getPackaging();

    /**
     * returns the charset that should be used when reading artifact, default is UTF-8 might be
     * overridden by subclasses
     *
     * @return the charset
     */
    protected Charset getPrimaryArtifactCharset() {
        return StandardCharsets.UTF_8;
    }

    protected abstract void initModel(Model model, Reader artifactReader, Path artifactFile) throws IOException;

    protected static Properties getBuildProperties(Path dir) throws IOException {
        return loadProperties(dir.resolve("build.properties"));
    }

    static Properties loadProperties(Path propertiesPath) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(propertiesPath)) {
            try (InputStream stream = Files.newInputStream(propertiesPath)) {
                properties.load(stream);
            }
        }
        return properties;
    }

    static Supplier<Properties> getPropertiesSupplier(Path propertiesFile) {
        return new Supplier<>() {
            private Properties properties;

            @Override
            public Properties get() {
                if (properties == null) {
                    try {
                        properties = loadProperties(propertiesFile);
                    } catch (IOException e) { // ignore externalized data and try again the next time
                        return new Properties();
                    }
                }
                return properties;
            }
        };
    }

    static String localizedValue(String value, Supplier<Properties> properties) {
        if (value != null && value.startsWith("%")) { //load translated value from properties
            String key = value.substring(1);
            String translation = properties.get().getProperty(key);
            return translation != null && !translation.isEmpty() ? translation : key;
        }
        return value;
    }

    @Override
    public Properties getEnhancementProperties(Map<String, ?> options) {
        Optional<Path> file = getLocation(options);
        try {
            if (file.isPresent()) {
                return getEnhancementProperties(file.get());
            }
        } catch (IOException e) {
            logger.warn("reading EnhancementProperties encountered a problem and was skipped for this reason", e);
        }
        return null;
    }

    protected Properties getEnhancementProperties(Path file) throws IOException {
        Path dir = Files.isDirectory(file) ? file : file.getParent();
        return getBuildProperties(dir);
    }

    private static void setLocation(Model model, Path modelSource) {
        InputSource inputSource = new InputSource();
        inputSource.setLocation(modelSource.toString());
        inputSource.setModelId(model.getParent().getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        model.setLocation("", new InputLocation(0, 0, inputSource));
    }

    protected String getPomVersion(String pdeVersion) {
        String pomVersion = pdeVersion;
        if (pdeVersion.endsWith(QUALIFIER_SUFFIX)) {
            String unqualifiedVersion = pdeVersion.substring(0, pdeVersion.length() - QUALIFIER_SUFFIX.length());
            if (isExtensionMode() && snapshotFormat != null) {
                return unqualifiedVersion + snapshotFormat;
            }
            return unqualifiedVersion + "-SNAPSHOT";
        }
        return pomVersion;
    }

    public boolean isExtensionMode() {
        return extensionMode;
    }

    public void setExtensionMode(boolean extensionMode) {
        this.extensionMode = extensionMode;

    }

    public void setMultiModuleProjectDirectory(File multiModuleProjectDirectory) {
        this.multiModuleProjectDirectory = multiModuleProjectDirectory;
    }

    public void setSnapshotFormat(String snapshotFormat) {
        this.snapshotFormat = snapshotFormat;
    }

    static Optional<Path> getLocation(Map<String, ?> options) {
        String location = PolyglotModelUtil.getLocation(options);
        if (location != null) {
            try {
                return Optional.of(Path.of(location));
            } catch (InvalidPathException e) {
            }
        }
        return Optional.empty();
    }

    static String getFileName(Path file) {
        return file.getFileName().toString();
    }
}
