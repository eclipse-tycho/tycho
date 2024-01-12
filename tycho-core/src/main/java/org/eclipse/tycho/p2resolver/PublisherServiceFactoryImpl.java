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
package org.eclipse.tycho.p2resolver;

import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.tools.publisher.PublishProductToolImpl;
import org.eclipse.tycho.p2.tools.publisher.PublisherActionRunner;
import org.eclipse.tycho.p2.tools.publisher.PublisherServiceImpl;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;

@Component(role = PublisherServiceFactory.class)
public class PublisherServiceFactoryImpl implements PublisherServiceFactory {

    @Requirement
    private MavenContext mavenContext;
    @Requirement
    private ReactorRepositoryManager reactorRepoManager;

    @Requirement
    private TargetPlatformService targetPlatformService;

    @Override
    public PublisherService createPublisher(ReactorProject project, List<TargetEnvironment> environments) {
        P2TargetPlatform targetPlatform = targetPlatformService.getTargetPlatform(project)
                .filter(P2TargetPlatform.class::isInstance).map(P2TargetPlatform.class::cast)
                .orElseThrow(() -> new IllegalStateException("Target platform is missing"));
        PublisherActionRunner publisherRunner = getPublisherRunnerForProject(targetPlatform, environments);
        PublishingRepository publishingRepository = reactorRepoManager.getPublishingRepository(project);

        return new PublisherServiceImpl(publisherRunner, project.getBuildQualifier(), publishingRepository);
    }

    @Override
    public PublishProductTool createProductPublisher(ReactorProject project, List<TargetEnvironment> environments,
            String buildQualifier, Interpolator interpolator) {
        P2TargetPlatform targetPlatform = targetPlatformService.getTargetPlatform(project)
                .filter(P2TargetPlatform.class::isInstance).map(P2TargetPlatform.class::cast)
                .orElseThrow(() -> new IllegalStateException("Target platform is missing"));
        PublisherActionRunner publisherRunner = getPublisherRunnerForProject(targetPlatform, environments);
        PublishingRepository publishingRepository = reactorRepoManager.getPublishingRepository(project);

        return new PublishProductToolImpl(publisherRunner, publishingRepository, targetPlatform, buildQualifier,
                interpolator, mavenContext.getLogger());
    }

    private PublisherActionRunner getPublisherRunnerForProject(P2TargetPlatform targetPlatform,
            List<TargetEnvironment> environments) {
        return new PublisherActionRunner(targetPlatform.getMetadataRepository(), environments,
                mavenContext.getLogger());
    }

}
