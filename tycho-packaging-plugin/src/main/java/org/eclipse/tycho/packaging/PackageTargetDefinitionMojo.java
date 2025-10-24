/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich -  Bug 461284 - Improve discovery and attach of .target files in eclipse-target-definition
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

/**
 * Registers all target definition files in the basedir of a project as maven artifact.
 * 
 */
@Mojo(name = "package-target-definition", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PackageTargetDefinitionMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Inject
    MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File[] targetFiles = TargetDefinitionFile.listTargetFiles(project.getBasedir());
        for (File targetFile : targetFiles) {
            if (DefaultTargetPlatformConfigurationReader.isPrimaryTarget(project, targetFile, targetFiles)) {
                project.getArtifact().setFile(targetFile);
            } else {
                projectHelper.attachArtifact(project, targetFile, FilenameUtils.getBaseName(targetFile.getName()));
            }
        }
        if (project.getArtifact().getFile() == null) {
            DefaultTargetPlatformConfigurationReader.throwNoPrimaryTargetFound(project, targetFiles);
        }

    }
}
