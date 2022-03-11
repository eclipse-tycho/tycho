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
import java.util.jar.Attributes;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;

import aQute.bnd.osgi.Jar;

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
                    try (Jar hostBundle = new Jar(project.getArtifact().getFile());
                            Jar sourceJar = new Jar(sourceFile)) {
                        Attributes hostMain = hostBundle.getManifest().getMainAttributes();
                        Attributes sourceMain = sourceJar.getManifest().getMainAttributes();
                        String hostName = hostMain.getValue(BUNDLE_SYMBOLICNAME);
                        String hostVersion = hostMain.getValue(BUNDLE_VERSION);
                        sourceMain.putValue(BUNDLE_MANIFESTVERSION, "2");
                        sourceMain.putValue(BUNDLE_SYMBOLICNAME, hostName + sourceBundleSuffix);
                        sourceMain.putValue(BUNDLE_VERSION, hostVersion);
                        sourceMain.putValue(OsgiSourceMojo.MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE,
                                hostName + ";version=\"" + hostVersion + "\";roots:=\".\"");
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

}
