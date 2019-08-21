/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Map;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.maven.polyglot.PolyglotModelManager;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.mapping.Mapping;

/**
 * Base implementation for a {@link Mapping} and {@link ModelReader} that handles all the low-level
 * stuff, implementations must only handle a small subset
 *
 */
public abstract class AbstractTychoMapping implements Mapping, ModelReader {

    private static final String QUALIFIER_SUFFIX = ".qualifier";
    private static final String ISSUE_192 = ".takari_issue_192";
    private static final String MODEL_PARENT = "TychoMapping.model.parent";

    @Requirement
    protected PlexusContainer container;

    @Requirement
    protected PolyglotModelManager polyglotModelManager;

    @Requirement
    protected Logger logger;

    private ModelWriter writer;

    @Override
    public File locatePom(File dir) {
        File file = new File(dir, "pom.xml");
        if (file.exists()) {
            //we wan't that pom.xml takes precedence over our generated one
            return null;
        }
        File artifact = getPrimaryArtifact(dir);
        if (artifact != null && artifact.getName().endsWith(".xml")) {
            File dummyFile = new File(dir, artifact.getName() + ISSUE_192);
            try {
                //due to https://github.com/takari/polyglot-maven/issues/192
                dummyFile.createNewFile();
                dummyFile.deleteOnExit();
                return dummyFile;
            } catch (IOException e) {
                throw new RuntimeException("creation of replacement file " + dummyFile + " failed", e);
            }
        }
        return artifact;
    }

    @Override
    public boolean accept(Map<String, ?> options) {
        String location = PolyglotModelUtil.getLocation(options);
        if (location == null || location.endsWith("pom.xml")) {
            //we wan't that pom.xml takes precedence over our generated one
            return false;
        }
        return isValidLocation(fixLocation(location));
    }

    private String fixLocation(String location) {
        if (location != null && location.endsWith(ISSUE_192)) {
            location = location.substring(0, location.length() - ISSUE_192.length());
        }
        return location;
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
        String fixLocation = fixLocation(location);
        File file = new File(fixLocation);
        if (!fixLocation.equals(location)) {
            //we must use the "fixed" location here until the issue is resolved ignoring the original input stream
            input = new FileInputStream(file);
        }
        return read(new InputStreamReader(input, getPrimaryArtifactCharset()), file, options);
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException, ModelParseException {
        File artifactFile = getRealArtifactFile(input);
        if (artifactFile.exists()) {
            try (FileInputStream stream = new FileInputStream(artifactFile)) {
                return read(new InputStreamReader(stream, getPrimaryArtifactCharset()), input, options);
            }
        } else {
            //we support the case here, that actually there is no real primary artifact, instead of creating a dummy file and forcing I/O we simply simulate to read a 0-byte file
            return read(new StringReader(""), input, options);
        }
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        File file = new File(fixLocation(PolyglotModelUtil.getLocation(options)));
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
            //if the parent is given by the options we don't neet to search it!
            return parent;
        }

        // assumption/limitation: parent pom must be physically located in
        // parent directory
        File parentPom = polyglotModelManager.locatePom(projectRoot.getParentFile());
        if (parentPom == null) {
            throw new FileNotFoundException("No parent pom file found in " + projectRoot.getParentFile());
        }
        Map<String, File> options = new HashMap<>(4);
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
        parentReference.setRelativePath("../" + parentPom.getName());
        return parentReference;
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

    private static void setLocation(Model model, File modelSource) {
        InputSource inputSource = new InputSource();
        inputSource.setLocation(modelSource.toString());
        inputSource.setModelId(model.getParent().getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        model.setLocation("", new InputLocation(0, 0, inputSource));
    }

    protected static String getPomVersion(String pdeVersion) {
        String pomVersion = pdeVersion;
        if (pdeVersion.endsWith(QUALIFIER_SUFFIX)) {
            pomVersion = pdeVersion.substring(0, pdeVersion.length() - QUALIFIER_SUFFIX.length()) + "-SNAPSHOT";
        }
        return pomVersion;
    }
}
