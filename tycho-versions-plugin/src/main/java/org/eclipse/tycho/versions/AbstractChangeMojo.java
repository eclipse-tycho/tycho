/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.VersionsEngine;

public abstract class AbstractChangeMojo extends AbstractMojo {

    /**
     * <p>
     * Initial list of of projects to be changed. From these projects, the full list of projects to
     * be changed is derived according to the rules described above. If set, this parameter needs to
     * be specified as a comma separated list of artifactIds.
     * </p>
     */
    @Parameter(property = "artifacts", defaultValue = "${project.artifactId}")
    private String artifacts;

    /**
     * List of additional modules (relative to the basedir) to considering even if not listed in the
     * pom, this can be used to update related projects that are not included as modules in the
     * root.
     */
    @Parameter(property = "modules")
    private String modules;

    @Inject
    private VersionsEngine engine;

    @Parameter(property = "session", readonly = true)
    protected MavenSession session;

    @Inject
    private ProjectMetadataReader metadataReader;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (engine) {
            synchronized (metadataReader) {
                try {
                    File basedir = session.getCurrentProject().getBasedir();
                    boolean recursive = session.getRequest().isRecursive();
                    metadataReader.addBasedir(basedir, recursive);
                    for (String p : split(modules)) {
                        metadataReader.addBasedir(new File(basedir, p), recursive);
                    }
                    engine.setProjects(metadataReader.getProjects());
                    addChanges(split(artifacts), engine);
                    engine.apply();
                    engine.reset();
                    metadataReader.reset();
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not set version", e);
                }
            }
        }
    }

    protected static List<String> split(String str) {
        ArrayList<String> result = new ArrayList<>();
        if (str != null && !str.isBlank()) {
            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }

    protected abstract void addChanges(List<String> artifacts, VersionsEngine engine)
            throws MojoExecutionException, IOException;
}
