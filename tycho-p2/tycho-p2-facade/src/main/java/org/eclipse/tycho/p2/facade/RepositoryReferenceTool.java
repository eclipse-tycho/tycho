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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
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

        addRepositoriesOfReferencedModules(repositories, module);

        repositories.addArtifactRepository(RepositoryBlackboardKey.forResolutionContextArtifacts(module.getBasedir()));

        // metadata and artifacts of target platform
        File targetPlatform = materializeTargetPlatformRepository(module);
        repositories.addMetadataRepository(targetPlatform);
        repositories.addArtifactRepository(new File(session.getLocalRepository().getBasedir()));
        return repositories;
    }

    private static void addRepositoriesOfReferencedModules(RepositoryReferences sources, MavenProject currentProject)
            throws MojoExecutionException, MojoFailureException {
        for (MavenProject referencedProject : currentProject.getProjectReferences().values()) {
            String packaging = referencedProject.getPackaging();
            if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                    || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)
                    || ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
                // check that expected repository files are there (for more descriptive problem messages)
                File metadataXml = getAttachedArtifact(referencedProject, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA);
                File artifactXml = getAttachedArtifact(referencedProject,
                        RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS);
                File artifactLocations = new File(artifactXml.getParentFile(),
                        RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
                if (!artifactLocations.isFile()) {
                    throw new MojoFailureException("Missing required file \"" + artifactLocations
                            + "\" in target folder of module " + referencedProject.getId());
                }

                sources.addMetadataRepository(metadataXml.getParentFile());
                sources.addArtifactRepository(artifactXml.getParentFile());
            }
        }
    }

    private static File getAttachedArtifact(MavenProject project, String classifier) throws MojoFailureException {
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (classifier.equals(artifact.getClassifier())) {
                return artifact.getFile();
            }
        }
        throw new MojoFailureException("Missing required artifact '" + classifier + "' in module " + project.getId());
    }

    /**
     * Restores the p2 metadata view on the module's build target platform (without reactor
     * projects) that was calculated during the initial dependency resolution (see
     * org.eclipse.tycho.p2.resolver.P2ResolverImpl.toResolutionResult(...)).
     */
    private File materializeTargetPlatformRepository(MavenProject module) throws MojoExecutionException,
            MojoFailureException {
        try {
            File repositoryLocation = new File(module.getBuild().getDirectory(), "targetPlatformRepository");
            repositoryLocation.mkdirs();
            FileOutputStream stream = new FileOutputStream(new File(repositoryLocation, "content.xml"));
            try {
                MetadataSerializable serializer = osgiServices.getService(MetadataSerializable.class);
                Set<?> targetPlatformInstallableUnits = TychoProjectUtils.getDependencyArtifacts(module)
                        .getNonReactorUnits();
                serializer.serialize(stream, targetPlatformInstallableUnits);
            } finally {
                stream.close();
            }
            return repositoryLocation;
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing the build target platform to disk", e);
        }
    }

}
