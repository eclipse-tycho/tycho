/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.p2tools.RepositoryReferenceTool;

import javax.inject.Inject;

public abstract class AbstractPublishMojo extends AbstractP2Mojo {
    private static final Object LOCK = new Object();

    @Inject
    private RepositoryReferenceTool repositoryReferenceTool;

    @Inject
    PublisherServiceFactory publisherServiceFactory;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            try {
                Collection<DependencySeed> units = publishContent(publisherServiceFactory);
                postPublishedIUs(units);
            } catch (Exception ex) {
                throw new MojoFailureException(
                        "Publisher failed. Verify your target-platform-configuration and executionEnvironment are suitable for proper resolution",
                        ex);
            }
        }
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
        getReactorProject().getDependencySeeds().addAll(units);
    }
}
