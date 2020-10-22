/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
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

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

/**
 * <p>
 * Publishes a custom execution environment profile.
 * </p>
 * 
 * @since 0.16.0
 */
@Mojo(name = "publish-ee-profile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public final class PublishEEProfileMojo extends AbstractPublishMojo {

    /**
     * <p>
     * The profile file containing the execution environment definition.
     * </p>
     */
    @Parameter(required = true)
    private File profileFile;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        PublisherService publisherService = publisherServiceFactory.createPublisher(getReactorProject(),
                getEnvironments());

        try {
            Collection<DependencySeed> ius = publisherService.publishEEProfile(profileFile);
            getLog().info("Published profile IUs: " + ius);
            return ius;
        } catch (FacadeException e) {
            throw new MojoExecutionException(
                    "Exception while publishing execution environment profile " + profileFile + ": " + e.getMessage(),
                    e);
        }
    }
}
