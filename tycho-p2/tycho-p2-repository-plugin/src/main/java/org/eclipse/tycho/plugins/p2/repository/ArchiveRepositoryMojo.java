/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * <p>
 * Creates a zip archive with the aggregated p2 repository.
 * </p>
 */
@Mojo(name = "archive-repository", defaultPhase = LifecyclePhase.PACKAGE)
public final class ArchiveRepositoryMojo extends AbstractRepositoryMojo {

    @Component(role = Archiver.class, hint = "zip")
    private Archiver inflater;

    /**
     * <p>
     * Name of the generated zip file (without extension).
     * </p>
     */
    @Parameter(property = "project.build.finalName")
    private String finalName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        File destFile = getBuildDirectory().getChild(finalName + ".zip");

        try {
            inflater.addDirectory(getAssemblyRepositoryLocation());
            inflater.setDestFile(destFile);
            inflater.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error packing p2 repository", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error packing p2 repository", e);
        }

        getProject().getArtifact().setFile(destFile);
    }

}
