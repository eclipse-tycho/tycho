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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
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

    private static final String TYCHO_POMLESS_PARENT = "tycho.pomless.parent";
    private static final String PARENT_POM_DEFAULT_VALUE = System.getProperty(TYCHO_POMLESS_PARENT, "..");
    private static final String QUALIFIER_SUFFIX = ".qualifier";
    private static final String MODEL_PARENT = "TychoMapping.model.parent";

    @Requirement
    protected PlexusContainer container;

    @Requirement
    protected Logger logger;

    private ModelWriter writer;
    private boolean extensionMode;
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
        String location = PolyglotModelUtil.getLocation(options);
        if (location == null || location.endsWith("pom.xml")) {
            //we want that pom.xml takes precedence over our generated one
            return false;
        }
        return isValidLocation(location);
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
    public Model read(InputStream input, Map<String, ?> options) throws IOException, ModelParseException {
        String location = PolyglotModelUtil.getLocation(options);
        File file = new File(location);
        try (InputStreamReader stream = new InputStreamReader(input, getPrimaryArtifactCharset())) {
            return read(stream, file, options);
        }
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException, ModelParseException {
        File artifactFile = getRealArtifactFile(input);
        if (artifactFile.exists()) {
            if (artifactFile.isDirectory()) {
                return read(new StringReader(""), input, options);
            }
            try (InputStreamReader stream = new InputStreamReader(new FileInputStream(artifactFile),
                    getPrimaryArtifactCharset())) {
                return read(stream, input, options);
            }
        } else {
            //we support the case here, that actually there is no real primary artifact, instead of creating a dummy file and forcing I/O we simply simulate to read a 0-byte file
            return read(new StringReader(""), input, options);
        }
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        File file = new File(PolyglotModelUtil.getLocation(options));
        return read(input, file, options);
    }

    private Model read(Reader artifactReader, File artifactFile, Map<String, ?> options)
            throws ModelParseException, IOException {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setPackaging(getPackaging());
        initModel(model, artifactReader, artifactFile);
        if (model.getParent() == null) {
            model.setParent(findParent(artifactFile.getParentFile(), options));
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

    protected File getRealArtifactFile(File polyglotArtifactFile) {
        return polyglotArtifactFile;
    }

    protected Parent findParent(File projectRoot, Map<String, ?> projectOptions)
            throws ModelParseException, IOException {
        Parent parent = (Parent) projectOptions.get(MODEL_PARENT);
        if (parent != null) {
            //if the parent is given by the options we don't need to search it!
            return parent;
        }
        Properties buildProperties = getBuildProperties(projectRoot);
        // assumption parent pom must be physically located in parent directory if not given by build.properties
        String parentRef = buildProperties.getProperty(TYCHO_POMLESS_PARENT, PARENT_POM_DEFAULT_VALUE);
        File fileOrFolder = new File(projectRoot, parentRef).getCanonicalFile();
        PomReference parentPom;
        if (fileOrFolder.isFile()) {
            parentPom = locatePomReference(fileOrFolder.getParentFile(), fileOrFolder.getName());
        } else if (fileOrFolder.isDirectory()) {
            parentPom = locatePomReference(fileOrFolder, null);
        } else {
            throw new FileNotFoundException("parent pom file/folder " + fileOrFolder + " is not accessible");
        }
        if (parentPom == null) {
            throw new FileNotFoundException("No parent pom file found in " + fileOrFolder.getCanonicalPath());
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
        parentReference.setRelativePath(
                projectRoot.getCanonicalFile().toPath().relativize(parentPom.getPomFile().toPath()).toString());
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
    protected PomReference locatePomReference(File folder, String nameHint) {
        PomReference reference = null;
        try {
            List<ModelProcessor> lookupList = container.lookupList(ModelProcessor.class);
            for (ModelProcessor processor : lookupList) {
                File pom = processor.locatePom(folder);
                if (pom != null && pom.exists()) {
                    if (reference == null || pom.getName().equals(nameHint)) {
                        reference = new PomReference(pom, processor);
                    }
                }
            }
        } catch (ComponentLookupException e) {
        }
        return reference;
    }

    @Override
    public float getPriority() {
        return 0;
    }

    @Override
    public String getFlavour() {
        return getPackaging();
    }

    protected abstract boolean isValidLocation(String location);

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

    protected abstract void initModel(Model model, Reader artifactReader, File artifactFile)
            throws ModelParseException, IOException;

    protected static Properties getBuildProperties(File dir) throws IOException {
        Properties properties = new Properties();
        File buildProperties = new File(dir, "build.properties");
        if (buildProperties.exists()) {
            try (FileInputStream stream = new FileInputStream(buildProperties)) {
                properties.load(stream);
            }
        }
        return properties;
    }

    @Override
    public Properties getEnhancementProperties(Map<String, ?> options) {
        String location = PolyglotModelUtil.getLocation(options);
        File file = new File(location);
        try {
            return getEnhancementProperties(file);
        } catch (IOException e) {
            logger.warn("reading EnhancementProperties encountered a problem and was skipped for this reason", e);
        }
        return null;
    }

    protected Properties getEnhancementProperties(File file) throws IOException {
        if (file.isDirectory()) {
            return getBuildProperties(file);
        }
        return getBuildProperties(file.getParentFile());
    }

    private static void setLocation(Model model, File modelSource) {
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
}
