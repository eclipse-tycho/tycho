/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.target;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

/**
 * Supports mirroring the computed target platform of the current project, this behaves similar to
 * what PDE offers with its export deployable feature / plug-in and assembles an update site that
 * contains everything this particular project depends on.
 */
@Mojo(name = "mirror-target-platform", threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class MirrorTargetPlatformMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/target-platform-repository")
    private File destination;

    @Parameter(defaultValue = "${project.id}")
    private String name;

    @Component
    private TargetPlatformService platformService;

    @Component
    private MirrorApplicationService mirrorService;

    @Component
    private ReactorRepositoryManager repositoryManager;

    @Component
    private IProvisioningAgent agent;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatform targetPlatform = platformService.getTargetPlatform(reactorProject).orElse(null);
        if (targetPlatform == null) {
            getLog().info("Project has no target platform, skip execution.");
            return;
        }
        IArtifactRepository sourceArtifactRepository = targetPlatform.getArtifactRepository();
        IMetadataRepository sourceMetadataRepository = targetPlatform.getMetadataRepository();
        PublishingRepository publishingRepository = repositoryManager.getPublishingRepository(reactorProject);
        getLog().info("Mirroring target platform, this can take a while ...");
        try {
            IArtifactRepository artifactRepository = new ListCompositeArtifactRepository(
                    List.of(sourceArtifactRepository, publishingRepository.getArtifactRepository()), agent);
            IMetadataRepository metadataRepository = new ListCompositeMetadataRepository(
                    List.of(sourceMetadataRepository, publishingRepository.getMetadataRepository()), agent);
            mirrorService.mirrorDirect(artifactRepository, metadataRepository, destination, name);
        } catch (FacadeException e) {
            throw new MojoFailureException(e.getMessage(), e.getCause());
        }
    }

}
