/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Issue #697 - Failed to resolve dependencies with Tycho 2.7.0 for custom repositories
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.repository.module.ModuleArtifactRepository;
import org.eclipse.tycho.p2.repository.module.ModuleMetadataRepository;
import org.eclipse.tycho.p2.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

@Component(role = ReactorRepositoryManager.class)
public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    @Requirement
    IProvisioningAgent agent;

    @Requirement
    P2Generator p2generator;

    @Override
    public PublishingRepository getPublishingRepository(ReactorProjectIdentities project) {
        return new PublishingRepositoryImpl(agent, project);
    }

    @Override
    public PublishingRepository getPublishingRepository(ReactorProject project) {

        File targetDir = project.getBuildDirectory().getLocation();
        if (!ModuleMetadataRepository.canAttemptRead(targetDir)
                || !ModuleArtifactRepository.canAttemptRead(targetDir)) {
            //no metadata there so just generate it...
            try {
                agent.getService(Object.class); //needed to make checksum computation work see https://github.com/eclipse-equinox/p2/issues/214
                p2generator.generateMetaData(project.adapt(MavenProject.class));
            } catch (Exception e) {
                // can't do anything then...
            }
        }

        return getPublishingRepository(project.getIdentities());
    }

}
