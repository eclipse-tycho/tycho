/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

package org.eclipse.tycho.source;

import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ReactorProject;
import org.osgi.framework.BundleException;

import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import org.slf4j.LoggerFactory;

/**
 * This mojo adds the required headers to a source artifact for it to be used in PDE as a source
 * bundle
 *
 */
@Mojo(name = "generate-pde-source-header", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PDESourceBundleMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "sourceBundleSuffix", defaultValue = ".source")
    private String sourceBundleSuffix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String packaging = project.getPackaging();
        if ("jar".equals(packaging) || "bundle".equals(packaging)) {
            for (Artifact artifact : project.getAttachedArtifacts()) {
                if (ReactorProject.SOURCE_ARTIFACT_CLASSIFIER.equalsIgnoreCase(artifact.getClassifier())
                        && "java-source".equals(artifact.getType())) {
                    File sourceFile = artifact.getFile();
                    File hostFile = project.getArtifact().getFile();
                    try (Jar hostBundle = new Jar(hostFile); Jar sourceJar = new Jar(sourceFile)) {
                        Attributes sourceMain = sourceJar.getManifest().getMainAttributes();
                        String hostName = hostBundle.getBsn();
                        String hostVersion = hostBundle.getVersion();

                        addBundleLocalicationFileIfAbsent(hostFile, hostName, sourceJar);

                        sourceMain.putValue(BUNDLE_MANIFESTVERSION, "2");
                        sourceMain.putValue(BUNDLE_SYMBOLICNAME, hostName + sourceBundleSuffix);
                        sourceMain.putValue(BUNDLE_VERSION, hostVersion);
                        sourceMain.putValue(OsgiSourceMojo.MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE,
                                hostName + ";version=\"" + hostVersion + "\";roots:=\".\"");

                        OsgiSourceMojo.addLocalicationHeaders(sourceMain::putValue);

                        String baseName = FilenameUtils.getBaseName(sourceFile.getName());
                        File outputFile = new File(sourceFile.getParentFile(), baseName + "-pde.jar");
                        sourceJar.write(outputFile);
                        artifact.setFile(outputFile);
                    } catch (Exception e) {
                        throw new MojoFailureException("Update of manifest failed!", e);
                    }
                }
            }
        }
    }

    private void addBundleLocalicationFileIfAbsent(File hostFile, String hostName, Jar sourceJar)
            throws BundleException, MojoExecutionException, IOException {
        if (sourceJar.getResource(OsgiSourceMojo.MANIFEST_BUNDLE_LOCALIZATION_FILENAME) == null) {
            try (FileSystem jarFS = FileSystems.newFileSystem(hostFile.toPath(), Map.of("create", "true"))) {
                Path jarRoot = jarFS.getRootDirectories().iterator().next();
                Map<String, String> headers = new CaseInsensitiveDictionaryMap<>();
                try (InputStream manifest = Files.newInputStream(jarRoot.resolve(JarFile.MANIFEST_NAME))) {
                    ManifestElement.parseBundleManifest(manifest, headers);
                }
                Resource l10n = OsgiSourceMojo.generateL10nFile(project, jarRoot, headers::get, hostName, LoggerFactory.getLogger(getClass()));
                Path file = Path.of(l10n.getDirectory()).resolve(OsgiSourceMojo.MANIFEST_BUNDLE_LOCALIZATION_FILENAME);
                sourceJar.putResource(OsgiSourceMojo.MANIFEST_BUNDLE_LOCALIZATION_FILENAME, new FileResource(file));
            }
        }
    }

}
