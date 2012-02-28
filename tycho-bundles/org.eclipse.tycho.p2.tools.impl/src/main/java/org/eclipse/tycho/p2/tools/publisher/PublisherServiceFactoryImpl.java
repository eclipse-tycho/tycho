/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class PublisherServiceFactoryImpl implements PublisherServiceFactory {

    private MavenContext mavenContext;
    private ReactorRepositoryManager reactorRepoManager;
    private BuildPropertiesParser buildPropertiesParser;

    public PublisherService createPublisher(RepositoryReferences contextRepos, BuildContext context)
            throws FacadeException {
        checkCollaborators();

        return new PublisherServiceImpl(context, new PublisherInfoTemplate(contextRepos, context,
                reactorRepoManager.getAgent()), reactorRepoManager.getPublishingRepository(context.getProject()), buildPropertiesParser,
                mavenContext.getLogger());
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setReactorRepositoryManager(ReactorRepositoryManager reactorRepoManager) {
        this.reactorRepoManager = reactorRepoManager;
    }

    public void setBuildPropertiesParser(BuildPropertiesParser buildPropertiesReader) {
        this.buildPropertiesParser = buildPropertiesReader;
    }

    private void checkCollaborators() {
        if (mavenContext == null || reactorRepoManager == null || buildPropertiesParser == null) {
            throw new IllegalStateException(); // shoudn't happen; see OSGI-INF/publisherfactory.xml
        }
    }

}
