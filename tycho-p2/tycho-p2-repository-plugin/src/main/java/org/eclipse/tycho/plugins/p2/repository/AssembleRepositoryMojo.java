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
 * <p>
 * Aggregates content into a p2 repository.
 * </p>
 * <p>
 * The aggregation runs recursively: it starts with the content published in the current module, and
 * traverses all artifacts that are marked as <em>included</em> in already aggregated artifacts.
 * (The following artifacts can <em>include</em> other artifacts: categories, products, and
 * features.)
 * </p>
 * 
 * @goal assemble-repository
 */
// TODO the goal should be called "aggregate-repository"
public class AssembleRepositoryMojo extends AbstractRepositoryMojo implements LogEnabled {
    /**
     * By default, this goal creates a p2 repository. Set this to <code>false</code> if only a p2
     * metadata repository (without the artifact files) shall be created.
     * 
     * @parameter default-value="true"
     */
    private boolean createArtifactRepository;

    /**
     * By default, only (transitive) <em>inclusions</em> of the published artifacts are aggregated.
     * Set this parameter to <code>true</code> to aggregate <em>all transitive dependencies</em>,
     * making the resulting p2 repository self-contained.
     * 
     * @parameter default-value="false"
     */
    private boolean includeAllDependencies;

    /**
     * Compress the repository index files <tt>content.xml</tt> and <tt>artifacts.xml</tt>.
     * 
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * The name attribute stored in the created p2 repository.
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
