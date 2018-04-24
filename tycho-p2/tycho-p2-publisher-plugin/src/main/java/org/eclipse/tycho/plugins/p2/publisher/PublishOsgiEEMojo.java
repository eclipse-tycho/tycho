package org.eclipse.tycho.plugins.p2.publisher;
/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

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
 * Publishes an OSGi execution environment profile into a p2 repository. The published IUs satisfy
 * dependencies to corresponding osgi.ee capabilities and system packages.
 * </p>
 * 
 * @since 1.2.0
 */
@Mojo(name = "publish-osgi-ee", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public final class PublishOsgiEEMojo extends AbstractPublishMojo {

    /**
     * <p>
     * The profile name. Examples: JavaSE-1.6, JavaSE-10, OSGi/Minimum-1.0.
     * </p>
     */
    @Parameter(defaultValue = "JavaSE-10")
    private String profileName;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        PublisherService publisherService = publisherServiceFactory.createPublisher(getReactorProject(),
                getEnvironments());
        try {
            Collection<DependencySeed> ius = publisherService.publishEEProfile(profileName);
            getLog().info("Published profile IUs: " + ius);
            return ius;
        } catch (FacadeException e) {
            throw new MojoExecutionException(
                    "Exception while publishing execution environment profile " + profileName + ": " + e.getMessage(),
                    e);
        }
    }
}
