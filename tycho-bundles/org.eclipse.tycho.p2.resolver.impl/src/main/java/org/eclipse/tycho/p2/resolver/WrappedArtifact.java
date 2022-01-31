/*******************************************************************************
 * Copyright (c) 2020, 2022 Christoph Läubrich and others.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Manifest;

import org.eclipse.tycho.p2.metadata.ArtifactFacadeProxy;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;

public final class WrappedArtifact extends ArtifactFacadeProxy {

    private static final String WRAPPED_CLASSIFIER = "wrapped";

    private final File file;
    private final String classifier;

    private String wrappedBsn;

    private String wrappedVersion;

    private Manifest manifest;

    private WrappedArtifact(File file, IArtifactFacade wrapped, String classifier, String wrappedBsn,
            String wrappedVersion, Manifest manifest) {
        super(wrapped);
        this.file = file;
        this.classifier = classifier;
        this.wrappedBsn = wrappedBsn;
        this.wrappedVersion = wrappedVersion;
        this.manifest = manifest;
    }

    @Override
    public File getLocation() {
        return file;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getPackagingType() {
        return "bundle";
    }

    public String getWrappedBsn() {
        return wrappedBsn;
    }

    public String getWrappedVersion() {
        return wrappedVersion;
    }

    public String getReferenceHint() {
        return "The artifact can be referenced in feature files with the following data: <plugin id=\"" + wrappedBsn
                + "\" version=\"" + wrappedVersion + "\" download-size=\"0\" install-size=\"0\" unpack=\"false\"/>";
    }

    public String getGeneratedManifest() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            manifest.write(bout);
        } catch (IOException e) {
            throw new AssertionError("should never happen", e);
        }
        return bout.toString(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "WrappedArtifact [file=" + file + ", wrapped=" + super.toString() + ", classifier=" + classifier + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(classifier, file);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WrappedArtifact other = (WrappedArtifact) obj;
        return Objects.equals(classifier, other.classifier) && Objects.equals(file, other.file);
    }

    public static WrappedArtifact createWrappedArtifact(IArtifactFacade mavenArtifact, String prefix, File wrappedFile)
            throws Exception {
        return createWrappedArtifact(mavenArtifact, createPropertiesForPrefix(prefix), wrappedFile);
    }

    public static WrappedArtifact createWrappedArtifact(IArtifactFacade mavenArtifact, Properties bndInstructions,
            File wrappedFile) throws Exception {
        String wrappedClassifier = WRAPPED_CLASSIFIER;
        String classifier = mavenArtifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            wrappedClassifier = classifier + "-" + WRAPPED_CLASSIFIER;
        }
        wrappedFile.getParentFile().mkdirs();
        try (Jar jar = new Jar(mavenArtifact.getLocation())) {
            Manifest originalManifest = jar.getManifest();
            try (Analyzer analyzer = new Analyzer()) {
                analyzer.setJar(jar);
                if (originalManifest != null) {
                    analyzer.mergeManifest(originalManifest);
                }

                analyzer.setProperty("mvnGroupId", mavenArtifact.getGroupId());
                analyzer.setProperty("mvnArtifactId", mavenArtifact.getArtifactId());
                analyzer.setProperty("mvnVersion", mavenArtifact.getVersion());
                analyzer.setProperty("mvnClassifier", Objects.requireNonNullElse(mavenArtifact.getClassifier(), ""));
                analyzer.setProperty("generatedOSGiVersion", createOSGiVersionFromArtifact(mavenArtifact).toString());
                analyzer.setProperties(bndInstructions);
                Manifest manifest = analyzer.calcManifest();
                jar.setManifest(manifest);
                jar.write(wrappedFile);
                return new WrappedArtifact(wrappedFile, mavenArtifact, wrappedClassifier,
                        manifest.getMainAttributes().getValue(Analyzer.BUNDLE_SYMBOLICNAME),
                        manifest.getMainAttributes().getValue(Analyzer.BUNDLE_VERSION), manifest);
            }
        }
    }

    public static Properties createPropertiesForPrefix(String prefix) {
        Properties properties = new Properties();
        properties.setProperty("Bundle-Name",
                "Bundle derived from maven artifact ${mvnGroupId}:${mvnArtifactId}:${mvnVersion}");
        properties.setProperty("Bundle-SymbolicName", prefix + ".${mvnGroupId}.${mvnArtifactId}");
        properties.setProperty("version", " ${version_cleanup;${mvnVersion}}");
        properties.setProperty("Bundle-Version", "${version}");
        properties.setProperty("Import-Package", "*;resolution:=optional");
        properties.setProperty("Export-Package", "*;version=\"${version}\";-noimport:=true");
        properties.setProperty("DynamicImport-Package", "*");
        return properties;
    }

    public static String createClassifierFromArtifact(IArtifactFacade mavenArtifact) {
        String classifier = mavenArtifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            return classifier + "-" + WRAPPED_CLASSIFIER;
        }
        return WRAPPED_CLASSIFIER;
    }

    public static String createBundleSymbolicNameFromArtifact(String prefix, IArtifactFacade mavenArtifact) {
        String generatedBsn = prefix + "." + mavenArtifact.getGroupId() + "." + mavenArtifact.getArtifactId();
        String classifier = mavenArtifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            generatedBsn = generatedBsn + "." + classifier;
        }
        return generatedBsn;
    }

    public static Version createOSGiVersionFromArtifact(IArtifactFacade mavenArtifact) {
        String version = mavenArtifact.getVersion();
        return createOSGiVersionFromMaven(version);
    }

    protected static Version createOSGiVersionFromMaven(String version) {
        if (version == null) {
            return Version.emptyVersion;
        }
        try {
            int index = version.indexOf('-');
            if (index > -1) {
                StringBuilder sb = new StringBuilder(version);
                sb.setCharAt(index, '.');
                return Version.parseVersion(sb.toString());
            }
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            return new Version(0, 0, 1, version);
        }
    }

}
