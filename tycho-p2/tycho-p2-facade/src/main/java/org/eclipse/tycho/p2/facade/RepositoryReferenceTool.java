/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformResolverFactory;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.metadata.MetadataSerializable;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;

/**
 * Tool to obtain the list of p2 repositories that contain the dependencies of a module.
 */
@Component(role = RepositoryReferenceTool.class)
public class RepositoryReferenceTool {
    /**
     * Option to indicate that the publisher results of the given module shall be included in the
     * list of repositories.
     */
    public static int REPOSITORIES_INCLUDE_CURRENT_MODULE = 1;

    public static String PUBLISHER_REPOSITORY_PATH = "publisherRepository";

    @Requirement
    private EquinoxServiceFactory osgiServices;

    @Requirement
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;

    /**
     * Returns the list of visible p2 repositories for the build of the current module. The list
     * includes the p2 repositories of the referenced reactor modules, the target platform, and
     * optionally the current module itself. The repositories are sorted in a reasonable order of
     * precedence, so if there should be duplicate installable units or artifacts, the hope is that
     * it is deterministic from which repository the unit or artifact is taken. The order is:
     * <ol>
     * <li>The publisher results of the current module (only if the flag
     * {@link #REPOSITORIES_INCLUDE_CURRENT_MODULE} is set),
     * <li>The results of the referenced reactor modules,
     * <li>The non-reactor content of the module's target platform.
     * </ol>
     * 
     * @param module
     *            The current Maven project
     * @param session
     *            The current Maven session
     * @param flags
     *            Options flags; supported flags are {@link #REPOSITORIES_INCLUDE_CURRENT_MODULE}
     * @return a {@link RepositoryReferences} instance with the repositories.
     * @throws MojoExecutionException
     *             in case of internal errors
     * @throws MojoFailureException
     *             in case required artifacts are missing
     */
    public RepositoryReferences getVisibleRepositories(MavenProject module, MavenSession session, int flags)
            throws MojoExecutionException, MojoFailureException {
        RepositoryReferences repositories = new RepositoryReferences();

        if ((flags & REPOSITORIES_INCLUDE_CURRENT_MODULE) != 0) {
            File publisherResults = new File(module.getBuild().getDirectory(), PUBLISHER_REPOSITORY_PATH);
            repositories.addMetadataRepository(publisherResults);
            repositories.addArtifactRepository(publisherResults);
        }

        repositories.addArtifactRepository(RepositoryBlackboardKey.forResolutionContextArtifacts(module.getBasedir()));

        // metadata and artifacts of target platform
        addTargetPlatformRepository(repositories, session, module);
        repositories.addArtifactRepository(new File(session.getLocalRepository().getBasedir()));
        return repositories;
    }

    /**
     * Restores the p2 metadata view on the module's build target platform that was calculated
     * during the initial dependency resolution (see
     * org.eclipse.tycho.p2.resolver.P2ResolverImpl.toResolutionResult(...)).
     */
    private void addTargetPlatformRepository(RepositoryReferences sources, MavenSession session, MavenProject project)
            throws MojoExecutionException, MojoFailureException {
        try {
            File repositoryLocation = new File(project.getBuild().getDirectory(), "targetPlatformRepository");
            repositoryLocation.mkdirs();
            FileOutputStream stream = new FileOutputStream(new File(repositoryLocation, "content.xml"));
            try {
                MetadataSerializable serializer = osgiServices.getService(MetadataSerializable.class);

                TargetPlatform targetPlatform = TychoProjectUtils.getTargetPlatform(project);

                TargetPlatformResolver resolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

                TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);

                DependencyResolverConfiguration resolverConfiguration = configuration
                        .getDependencyResolverConfiguration();

                DependencyArtifacts dependencyArtifacts = resolver.resolveDependencies(session, project,
                        targetPlatform, DefaultReactorProject.adapt(session), resolverConfiguration);

                // this contains dependency-only metadata for 'this' project
                Set<Object> targetPlatformInstallableUnits = new HashSet<Object>(
                        dependencyArtifacts.getInstallableUnits());

                for (ArtifactDescriptor artifact : dependencyArtifacts.getArtifacts()) {
                    ReactorProject otherProject = artifact.getMavenProject();
                    if (otherProject == null) {
                        continue; // can't really happen
                    }
                    if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(otherProject.getPackaging())
                            || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(otherProject.getPackaging())
                            || ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(otherProject.getPackaging())) {
                        File artifactXml = otherProject.getArtifact(RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS);
                        if (artifactXml == null || !artifactXml.isFile()) {
                            throw new MojoFailureException("Missing required file \""
                                    + RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS
                                    + "\" in target folder of module " + otherProject.getId());
                        }
                        sources.addArtifactRepository(artifactXml.getParentFile());
                    }
                }

                serializer.serialize(stream, targetPlatformInstallableUnits);
            } finally {
                stream.close();
            }
            sources.addMetadataRepository(repositoryLocation);
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing the build target platform to disk", e);
        }
    }

}
