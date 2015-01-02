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
package org.eclipse.tycho.p2.tools.publisher;

import java.util.List;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class PublisherServiceFactoryImpl implements PublisherServiceFactory {

    private MavenContext mavenContext;
    private ReactorRepositoryManager reactorRepoManager;

    @Override
    public PublisherService createPublisher(ReactorProject project, List<TargetEnvironment> environments) {
        checkCollaborators();

        return new PublisherServiceImpl(new PublisherInfoTemplate(
                reactorRepoManager.getFinalTargetPlatformMetadataRepository(project), environments),
                project.getBuildQualifier(), reactorRepoManager.getPublishingRepository(project.getIdentities()),
                mavenContext.getLogger());
    }

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
