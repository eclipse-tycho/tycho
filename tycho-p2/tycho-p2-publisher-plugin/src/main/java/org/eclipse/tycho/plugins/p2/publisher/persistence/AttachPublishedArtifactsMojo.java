/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher.persistence;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.plugins.p2.publisher.AbstractP2Mojo;
import org.eclipse.tycho.repository.registry.facade.PublishingRepositoryFacade;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

/**
 * <p>
 * Attaches p2 metadata and extra artifacts produced by the publishers to the project. In this way,
 * the full build results are available in the local Maven repository, e.g. for builds of parts of a
 * reactor.
 * </p>
 * 
 * @goal attach-artifacts
 */
public class AttachPublishedArtifactsMojo extends AbstractP2Mojo {

    /** @component */
    private MavenProjectHelper projectHelper;

    /** @component */
    private EquinoxServiceFactory osgiServices;

    public void execute() throws MojoExecutionException, MojoFailureException {

        ReactorProject reactorProject = DefaultReactorProject.adapt(getProject());

        ReactorRepositoryManagerFacade reactorRepoManager = osgiServices
                .getService(ReactorRepositoryManagerFacade.class);
        PublishingRepositoryFacade publishingRepo = reactorRepoManager.getPublishingRepository(getProjectCoordinates());
        Map<String, File> artifacts = publishingRepo.getArtifactLocations();

        for (Entry<String, File> entry : artifacts.entrySet()) {
            String classifier = entry.getKey();
            File artifactLocation = entry.getValue();
            if (classifier == null) {
                getProject().getArtifact().setFile(artifactLocation);

            } else {
                String type = getExtension(artifactLocation);
                projectHelper.attachArtifact(getProject(), type, classifier, artifactLocation);

                // TODO does this still need to be set correctly at this point in the build?
                reactorProject.setDependencyMetadata(classifier, true, Collections.emptySet());
                reactorProject.setDependencyMetadata(classifier, false, Collections.emptySet());
            }
        }

        reactorProject.setDependencyMetadata(null, true, publishingRepo.getInstallableUnits());
        reactorProject.setDependencyMetadata(null, false, Collections.emptySet());
    }

    private static String getExtension(File file) {
        String fileName = file.getName();
        int separator = fileName.lastIndexOf('.');
        if (separator < 0) {
            throw new IllegalArgumentException("No file extension in \"" + fileName + "\"");
        }
        return fileName.substring(separator + 1);
    }

}
