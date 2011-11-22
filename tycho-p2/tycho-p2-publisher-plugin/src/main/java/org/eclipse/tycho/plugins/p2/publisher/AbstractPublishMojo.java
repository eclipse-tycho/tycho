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
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

public abstract class AbstractPublishMojo extends AbstractP2Mojo {

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    /** @component */
    private EquinoxServiceFactory osgiServices;

    public final void execute() throws MojoExecutionException, MojoFailureException {
        PublisherService publisherService = createPublisherService();
        try {
            Collection<?> units = publishContent(publisherService);
            postPublishedIUs(units);
        } finally {
            publisherService.stop();
        }
    }

    /**
     * Publishes source files with the help of the given publisher service.
     * 
     * @param publisherService
     * @return the list of root installable units that has been published
     */
    protected abstract Collection<?/* IInstallableUnit */> publishContent(PublisherService publisherService)
            throws MojoExecutionException, MojoFailureException;

    private PublisherService createPublisherService() throws MojoExecutionException, MojoFailureException {
        try {
            RepositoryReferences contextRepositories = repositoryReferenceTool.getVisibleRepositories(getProject(),
                    getSession(), 0);

            PublisherServiceFactory publisherServiceFactory = osgiServices.getService(PublisherServiceFactory.class);
            File publisherRepoLocation = getBuildDirectory().getChild(
                    RepositoryReferenceTool.PUBLISHER_REPOSITORY_PATH);
            return publisherServiceFactory.createPublisher(publisherRepoLocation, contextRepositories,
                    getBuildContext());
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while initializing the publisher service", e);
        }
    }

    /**
     * Adds the just published installable units into a shared list. The assemble-repository goal
     * eventually uses the units in that list as entry-points for mirroring content into the
     * assembly p2 repository.
     */
    private void postPublishedIUs(Collection<?> units) {
        final MavenProject project = getProject();
        // TODO use own type for this
        List<Object> publishedIUs = (List<Object>) project.getContextValue(TychoConstants.CTX_PUBLISHED_ROOT_IUS);
        if (publishedIUs == null) {
            publishedIUs = new ArrayList<Object>();
            project.setContextValue(TychoConstants.CTX_PUBLISHED_ROOT_IUS, publishedIUs);
        }
        publishedIUs.addAll(units);
    }
}
