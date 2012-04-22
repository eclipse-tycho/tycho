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
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;

/**
 * @goal assemble-repository
 */
public class AssembleRepositoryMojo extends AbstractRepositoryMojo implements LogEnabled {
    /**
     * Defines whether the artifacts of the included products, features, and bundles shall be
     * assembled into a p2 artifact repository. If <code>false</code>, only a p2 metadata repository
     * is created.
     * 
     * @parameter default-value="true"
     */
    private boolean createArtifactRepository;

    /**
     * Defines whether all transitive dependencies shall be included in the resulting repository. By
     * default, only features and bundles directly referenced in products and categories and their
     * "include" dependencies will be included in the result. To build a completely self-contained
     * repository, set this parameter to <code>true</code>.
     * 
     * @parameter default-value="false"
     */
    private boolean includeAllDependencies;

    /**
     * Defines whether the resulting p2 metadata should be compressed.
     * 
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * Defines the name of the p2 repository. The default value is the project name.
     * 
     * @parameter default-value="${project.name}"
     */
    private String repositoryName;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    /** @component */
    private EquinoxServiceFactory p2;

    private Logger logger;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File destination = getAssemblyRepositoryLocation();
            destination.mkdirs();

            Collection<?> rootIUs = (Collection<?>) getProject().getContextValue(TychoConstants.CTX_PUBLISHED_ROOT_IUS);
            if (rootIUs == null || rootIUs.size() == 0) {
                throw new MojoFailureException("No content specified for p2 repository");
            }

            RepositoryReferences sources = getVisibleRepositories();

            TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(getProject());

            MirrorApplicationService mirrorApp = p2.getService(MirrorApplicationService.class);
            DestinationRepositoryDescriptor destinationRepoDescriptor = new DestinationRepositoryDescriptor(
                    destination, repositoryName, compress, !createArtifactRepository, true);
            mirrorApp.mirrorReactor(sources, destinationRepoDescriptor, rootIUs, getBuildContext(),
                    includeAllDependencies, configuration.isIncludePackedArtifacts());
        } catch (FacadeException e) {
            throw new MojoExecutionException("Could not assemble p2 repository", e);
        }
    }

    protected RepositoryReferences getVisibleRepositories() throws MojoExecutionException, MojoFailureException {
        int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
        return repositoryReferenceTool.getVisibleRepositories(getProject(), getSession(), flags);
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }
}
