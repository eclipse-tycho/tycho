/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher.persistence;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.repository.registry.facade.PublishingRepositoryFacade;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

/**
 * <p>
 * Attaches p2 metadata and extra artifacts produced by the publishers to the project. In this way,
 * the full build results are available in the local Maven repository, e.g. for builds of parts of a
 * reactor.
 * </p>
 */
@Mojo(name = "attach-artifacts", threadSafe = true)
public class AttachPublishedArtifactsMojo extends AbstractP2Mojo {
    private static final Object LOCK = new Object();

    @Component
    private MavenProjectHelper projectHelper;

    @Component(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory osgiServices;

    @Component
    private Logger logger;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            ReactorRepositoryManagerFacade reactorRepoManager = osgiServices
                    .getService(ReactorRepositoryManagerFacade.class);
            PublishingRepositoryFacade publishingRepo = reactorRepoManager
                    .getPublishingRepository(getProjectIdentities());
            Map<String, File> artifacts = publishingRepo.getArtifactLocations();

            for (Entry<String, File> entry : artifacts.entrySet()) {
                String classifier = entry.getKey();
                File artifactLocation = entry.getValue();
                if (classifier == null) {
                    getProject().getArtifact().setFile(artifactLocation);
                } else {
                    String type = getExtension(artifactLocation);
                    projectHelper.attachArtifact(getProject(), type, classifier, artifactLocation);
                    logger.debug("Attaching " + type + "::" + classifier + " -> " + artifactLocation);
                }
            }

            ReactorProject reactorProject = getReactorProject();
            reactorProject.setDependencyMetadata(DependencyMetadataType.SEED, publishingRepo.getInstallableUnits());
            reactorProject.setDependencyMetadata(DependencyMetadataType.RESOLVE, Collections.emptySet());
        }
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
