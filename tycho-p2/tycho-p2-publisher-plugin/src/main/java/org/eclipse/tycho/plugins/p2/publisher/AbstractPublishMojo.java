/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

public abstract class AbstractPublishMojo extends AbstractP2Mojo {

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    private EquinoxServiceFactory osgiServices;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        PublisherServiceFactory publisherServiceFactory = osgiServices.getService(PublisherServiceFactory.class);
        Collection<DependencySeed> units = publishContent(publisherServiceFactory);
        postPublishedIUs(units);
    }

    /**
     * Publishes source files with the help of the given publisher service.
     * 
     * @return the list of root installable units that has been published
     */
    protected abstract Collection<DependencySeed> publishContent(PublisherServiceFactory publisherFactory)
            throws MojoExecutionException, MojoFailureException;

    /**
     * Adds the just published installable units into a shared list. The assemble-repository goal
     * eventually uses the units in that list as entry-points for mirroring content into the
     * assembly p2 repository.
     */
    private void postPublishedIUs(Collection<DependencySeed> units) {
        TychoProjectUtils.getDependencySeeds(getProject()).addAll(units);
    }
}
