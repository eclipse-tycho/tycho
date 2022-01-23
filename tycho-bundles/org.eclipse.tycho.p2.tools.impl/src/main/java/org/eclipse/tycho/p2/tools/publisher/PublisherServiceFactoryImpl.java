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
package org.eclipse.tycho.p2.tools.publisher;

import java.util.List;

import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.target.P2TargetPlatform;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class PublisherServiceFactoryImpl implements PublisherServiceFactory {

    private MavenContext mavenContext;
    private ReactorRepositoryManager reactorRepoManager;

    @Override
    public PublisherService createPublisher(ReactorProject project, List<TargetEnvironment> environments) {
        P2TargetPlatform targetPlatform = (P2TargetPlatform) reactorRepoManager.getFinalTargetPlatform(project);
        PublisherActionRunner publisherRunner = getPublisherRunnerForProject(targetPlatform, environments);
        PublishingRepository publishingRepository = reactorRepoManager.getPublishingRepository(project.getIdentities());

        return new PublisherServiceImpl(publisherRunner, project.getBuildQualifier(), publishingRepository);
    }

    @Override
    public PublishProductTool createProductPublisher(ReactorProject project, List<TargetEnvironment> environments,
            String buildQualifier, Interpolator interpolator) {
        P2TargetPlatform targetPlatform = (P2TargetPlatform) reactorRepoManager.getFinalTargetPlatform(project);
        PublisherActionRunner publisherRunner = getPublisherRunnerForProject(targetPlatform, environments);
        PublishingRepository publishingRepository = reactorRepoManager.getPublishingRepository(project.getIdentities());

        return new PublishProductToolImpl(publisherRunner, publishingRepository, targetPlatform, buildQualifier,
                interpolator, mavenContext.getLogger());
    }

    private PublisherActionRunner getPublisherRunnerForProject(P2TargetPlatform targetPlatform,
            List<TargetEnvironment> environments) {
        checkCollaborators();

        return new PublisherActionRunner(targetPlatform.getInstallableUnitsAsMetadataRepository(), environments,
                mavenContext.getLogger());
    }

    // setters for DS

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setReactorRepositoryManager(ReactorRepositoryManager reactorRepoManager) {
        this.reactorRepoManager = reactorRepoManager;
    }

    private void checkCollaborators() {
        if (mavenContext == null || reactorRepoManager == null) {
            throw new IllegalStateException(); // shoudn't happen; see OSGI-INF/publisherfactory.xml
        }
    }

}
