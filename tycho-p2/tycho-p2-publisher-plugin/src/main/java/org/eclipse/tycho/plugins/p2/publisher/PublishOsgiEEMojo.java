/*******************************************************************************
 * Copyright (c) 2018, 2021 SAP SE and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

/**
 * <p>
 * Publishes OSGi execution environment profiles into a p2 repository. The published IUs satisfy
 * dependencies to corresponding osgi.ee capabilities and system packages.
 * </p>
 * 
 * @since 1.2.0
 */
@Mojo(name = "publish-osgi-ee", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public final class PublishOsgiEEMojo extends AbstractPublishMojo {

    /**
     * <p>
     * Comma-separated list of profile names to be published. Examples: JavaSE-11, JavaSE-16,
     * JavaSE-17.
     * 
     * It is advised to keep this list as small as possible and the list must include the BREE used
     * by the platform, last Java LTS and the latest Java release.
     * </p>
     */
    @Parameter(defaultValue = "JavaSE-11, JavaSE-16, JavaSE-17")
    private String profiles;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Component
    private ToolchainManager toolchainManager;

    @Component
    private Logger logger;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return Collections.emptyList();
        }
        PublisherService publisherService = publisherServiceFactory.createPublisher(getReactorProject(),
                getEnvironments());
        Collection<DependencySeed> result = new ArrayList<>();
        for (String profile : profiles.split(",")) {
            try {
                profile = profile.trim();
                if (profile.isEmpty()) {
                    continue;
                }
                ExecutionEnvironment ee = ExecutionEnvironmentUtils.getExecutionEnvironment(profile, toolchainManager,
                        getSession(), logger);
                Collection<DependencySeed> ius = publisherService.publishEEProfile(ee);
                getLog().info("Published profile IUs: " + ius);
                result.addAll(ius);
            } catch (FacadeException e) {
                throw new MojoExecutionException(
                        "Exception while publishing execution environment profile " + profile + ": " + e.getMessage(),
                        e);
            }
        }
        return result;
    }

}
