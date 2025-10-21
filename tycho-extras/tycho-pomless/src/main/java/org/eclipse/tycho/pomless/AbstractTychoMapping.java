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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.maven.model.Build;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import javax.inject.Inject;

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.PlexusContainer;
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

    protected static final String TYCHO_GROUP_ID = "org.eclipse.tycho";

    // All build.properties entries specifically considered by Tycho. Extends the list in Mapping interface
    protected static final String TYCHO_POMLESS_PARENT_PROPERTY = "tycho.pomless.parent";
    protected static final String TYCHO_POMLESS_AGGREGATOR_NAMES_PROPERTY = "tycho.pomless.aggregator.names";

    private static final String PARENT_POM_DEFAULT_VALUE = System.getProperty(TYCHO_POMLESS_PARENT_PROPERTY, "..");
    private static final String QUALIFIER_SUFFIX = ".qualifier";

    private Map<Path, ParentModel> parentModelCache = new HashMap<Path, ParentModel>();

    @Inject
    protected PlexusContainer container;

    @Inject
    protected Logger logger;

    private ModelWriter writer;
    private boolean extensionMode;
    @SuppressWarnings("unused")
    private File multiModuleProjectDirectory;
    private String snapshotProperty;

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
            model.setParent(findParent(artifactFile.getParent(), options).parentReference());
        }
        if (model.getGroupId() == null && model.getParent() == null) {
            //if nothing has init this yet set at least a value
            model.setGroupId(artifactFile.getParent().getFileName().toString());
        }
        if (model.getVersion() == null && model.getParent() != null) {
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

    protected synchronized ParentModel findParent(Path projectRootIn, Map<String, ?> projectOptions)
            throws IOException {
        var projectRoot = projectRootIn.normalize();
        ParentModel cached = parentModelCache.get(projectRoot);
        if (cached != null) {
            return cached;
        }
        Properties buildProperties = getBuildProperties(projectRoot);
        // assumption parent pom must be physically located in parent directory if not given by build.properties
        String parentRef = buildProperties.getProperty(TYCHO_POMLESS_PARENT_PROPERTY, PARENT_POM_DEFAULT_VALUE);
        Path fileOrFolder = projectRoot.resolve(parentRef).toRealPath();
        return loadParent(projectRoot, fileOrFolder);
    }

    protected ParentModel loadParent(Path projectRoot, Path fileOrFolder) throws NoParentPomFound, IOException {
        PomReference parentPom;
        if (Files.isRegularFile(fileOrFolder)) {
            parentPom = locatePomReference(fileOrFolder.getParent(), getFileName(fileOrFolder));
        } else if (Files.isDirectory(fileOrFolder)) {
            parentPom = locatePomReference(fileOrFolder, null);
        } else {
            throw new FileNotFoundException("parent pom file/folder " + fileOrFolder + " is not accessible");
        }
        if (parentPom == null) {
            throw new NoParentPomFound(fileOrFolder);
        }
        Map<String, Object> options = new HashMap<>(1);
        options.put(ModelProcessor.SOURCE, new FileModelSource(parentPom.getPomFile()));
        Model parentModel = parentPom.getReader().read(parentPom.getPomFile(), options);
        Parent parentReference = new Parent();
        String groupId = parentModel.getGroupId();
        Parent grandParent = parentModel.getParent();
        if (groupId == null && grandParent != null) {
            // must be inherited from grandparent
            groupId = grandParent.getGroupId();
        }
        parentReference.setGroupId(groupId);
        parentReference.setArtifactId(parentModel.getArtifactId());
        String version = parentModel.getVersion();
        if (version == null && grandParent != null) {
            // must be inherited from grandparent
            version = grandParent.getVersion();
        }
        parentReference.setVersion(version);
        parentReference
                .setRelativePath(projectRoot.toRealPath().relativize(parentPom.getPomFile().toPath()).toString());
        logger.debug("Derived parent for path " + projectRoot + " is groupId: " + parentReference.getGroupId()
                + ", artifactId: " + parentReference.getArtifactId() + ", relativePath: "
                + parentReference.getRelativePath());
        ParentModel model = new ParentModel(parentReference, parentModel);
        parentModelCache.put(projectRoot, model);
        return model;
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
            logger.warn("Reading EnhancementProperties encountered a problem and was skipped for this reason", e);
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
        String groupId = model.getGroupId();
        if (groupId == null) {
            Parent parent = model.getParent();
            if (parent == null) {
                groupId = "-";
            } else {
                groupId = parent.getGroupId();
            }
        }
        inputSource.setModelId(groupId + ":" + model.getArtifactId() + ":" + model.getVersion());
        model.setLocation("", new InputLocation(0, 0, inputSource));
    }

    protected String getPomVersion(String pdeVersion, Model model, Path projectRoot) {
        String pomVersion = pdeVersion;
        if (pdeVersion.endsWith(QUALIFIER_SUFFIX)) {
            String unqualifiedVersion = pdeVersion.substring(0, pdeVersion.length() - QUALIFIER_SUFFIX.length());
            //we need to check that this property is actually defined!
            if (isExtensionMode() && modelHasProperty(snapshotProperty, model, projectRoot)) {
                return unqualifiedVersion + "${" + snapshotProperty + "}";
            }
            return unqualifiedVersion + "-SNAPSHOT";
        }
        return pomVersion;
    }

    private boolean modelHasProperty(String property, Model model, Path projectRoot) {
        if (property == null) {
            //nothing we can check assume it is NOT present...
            return false;
        }
        Properties properties = model.getProperties();
        String string = properties.getProperty(property);
        if (string != null) {
            return true;
        }
        try {
            ParentModel parent = findParent(projectRoot.getParent(), Map.of());
            Model parentModel = parent.parentModel();
            if (parentModel != null && parentModel != model) {
                return modelHasProperty(property, parentModel,
                        projectRoot.resolve(parent.parentReference().getRelativePath()));
            }
        } catch (IOException e) {
            //in this case we can't find the parent or there is no more parent...
        }
        return false;
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

    public void setSnapshotProperty(String snapshotFormat) {
        this.snapshotProperty = snapshotFormat;
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

    protected static String getFileName(Path file) {
        return file.getFileName().toString();
    }

    protected static Plugin disablePluginExecution(Model model, String groupId, String artifactId, String executionId) {

        Plugin plugin = getPlugin(model, groupId, artifactId);
        PluginExecution execution = new PluginExecution();
        execution.setId(executionId);
        execution.setPhase("none");
        plugin.addExecution(execution);
        return plugin;
    }

    protected static Plugin addPluginExecution(Plugin plugin, Consumer<PluginExecution> init) {
        PluginExecution execution = new PluginExecution();
        init.accept(execution);
        plugin.addExecution(execution);
        return plugin;
    }

    protected static Plugin getPlugin(Model model, String groupId, String artifactId) {
        Build build = getBuild(model);
        for (Plugin existing : build.getPlugins()) {
            if (existing.getGroupId().equals(groupId) && existing.getArtifactId().equals(artifactId)) {
                return existing;
            }
        }
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        build.addPlugin(plugin);
        return plugin;
    }

    protected static Build getBuild(Model model) {
        Build build = model.getBuild();
        if (build == null) {
            model.setBuild(build = new Build());
        }
        return build;
    }

    protected static MavenConfiguation getConfiguration(PluginExecution execution) {
        Object config = execution.getConfiguration();
        MavenConfiguation mavenConfiguation = new MavenConfiguation(config, "configuration");
        if (config == null) {
            execution.setConfiguration(mavenConfiguation.getXpp3());
        }
        return mavenConfiguation;
    }
}
