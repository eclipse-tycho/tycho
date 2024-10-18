/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Michael Pellaton (Netcetera) - add finalName mojo parameter
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.tycho.FileLockService;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * <p>
 * Creates a zip archive with the aggregated p2 repository.
 * </p>
 */
@Mojo(name = "archive-repository", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class ArchiveRepositoryMojo extends AbstractRepositoryMojo {

    @Inject
    private Provider<ZipArchiver> inflaterProvider;

    /**
     * <p>
     * Name of the generated zip file (without extension).
     * </p>
     */
    @Parameter(property = "project.build.finalName")
    private String finalName;

    /**
     * Whether or not to skip archiving the repository. False by default.
     */
    @Parameter(defaultValue = "false")
    private boolean skipArchive;

    @Inject
    private FileLockService fileLockService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipArchive) {
            return;
        }
        File repositoryLocation = getAssemblyRepositoryLocation();
        File destFile = getBuildDirectory().getChild(finalName + ".zip");
        try (var repoLock = fileLockService.lockVirtually(repositoryLocation);
                var destLock = fileLockService.lockVirtually(destFile);) {
            ZipArchiver inflater = inflaterProvider.get();
            inflater.addFileSet(DefaultFileSet.fileSet(repositoryLocation).prefixed(""));
            inflater.setDestFile(destFile);
            inflater.createArchive();
        } catch (ArchiverException | IOException e) {
            throw new MojoExecutionException("Error packing p2 repository", e);
        }
        getProject().getArtifact().setFile(destFile);
    }

}
