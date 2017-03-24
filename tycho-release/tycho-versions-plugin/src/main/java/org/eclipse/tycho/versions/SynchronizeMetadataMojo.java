/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - update version ranges
 *    Pere Joseph Rodríguez - synchronize metadata with pom version
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.SynchronizeVersionsEngine;

/**
 * <p>
 * Synchronizes the metadata version of the current project and child projects with their
 * corresponding pom version.
 * </p>
 * <p>
 * Allows updating properties.
 * </p>
 * <p>
 * By default bounds of OSGI version ranges referencing the version of an element that changed
 * version will be updated to match the newVersion.
 * </p>
 * <p>
 * Only executes in the root project of an aggregation project, due to it updates all child
 * projects, it's not necessary to execute again in child projects.
 * </p>
 */
@Mojo(name = "synchronize-version", aggregator = true, requiresDirectInvocation = true, inheritByDefault = false)
public class SynchronizeMetadataMojo extends AbstractVersionsMojo {

    /**
     * <p>
     * When true bounds of OSGI version ranges referencing the version of an element that changed
     * version will be updated to match the newVersion.
     * </p>
     */
    @Parameter(property = "updateVersionRangeMatchingBounds", defaultValue = "true")
    private boolean updateVersionRangeMatchingBounds;

    /**
     * <p>
     * Comma separated list of names of POM properties to set the new version to. Note that
     * properties are only changed in the projects explicitly listed by the {@link #artifacts}
     * parameter.
     * </p>
     * 
     * @since 0.18.0
     */
    @Parameter(property = "properties")
    private String properties;

    /**
     * skip inherited goal execution
     */
    @Parameter(property = "skipExecution", defaultValue = "false")
    private boolean skipExecution;

    /**
     * baseDir of the current execution
     */
    @Parameter(property = "basedir", readonly = true, defaultValue = "${project.basedir}")
    protected File basedir;

    /**
     * Check if the current goal is executing for the root project of the invocation
     * 
     * @return
     * @throws MojoExecutionException
     */
    private boolean isReactorRootProject() throws MojoExecutionException {
        try {
            String executionRootPath = new File(session.getExecutionRootDirectory()).getCanonicalFile()
                    .getAbsolutePath();
            String basedirPath = basedir.getCanonicalFile().getAbsolutePath();
            return executionRootPath.equals(basedirPath);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skipExecution && isReactorRootProject()) {
            SynchronizeVersionsEngine engine = newEngine();
            engine.setUpdateVersionRangeMatchingBounds(updateVersionRangeMatchingBounds);
            ProjectMetadataReader metadataReader = newProjectMetadataReader();

            try {
                metadataReader.addBasedir(session.getCurrentProject().getBasedir());

                engine.setProjects(metadataReader.getProjects());

                engine.apply();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not set version", e);
            }
        }
    }

    private SynchronizeVersionsEngine newEngine() throws MojoFailureException {
        return lookup(SynchronizeVersionsEngine.class);
    }

    private static List<String> split(String str) {
        ArrayList<String> result = new ArrayList<>();
        if (str != null) {
            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }
}
