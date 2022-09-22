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
package org.eclipse.tycho.p2.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.shared.MavenArtifactRepositoryReference;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.PreliminaryTargetPlatformImpl;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ReactorRepositoryManager.class, ReactorRepositoryManagerFacade.class })
public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    private static final String PRELIMINARY_TARGET_PLATFORM_KEY = ReactorRepositoryManagerImpl.class.getName()
            + "/dependencyOnlyTargetPlatform";

    private IProvisioningAgentProvider agentFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    private TargetPlatformFactory tpFactory;

    @Reference
    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    @Reference
    public void bindP2ResolverFactory(P2ResolverFactory p2ResolverFactory) {
        tpFactory = p2ResolverFactory.getTargetPlatformFactory();
    }

    @Activate
    public void activateManager() throws IOException, ProvisionException {
        agentDir = createTempDir("tycho_reactor_agent");
        agent = agentFactory.createAgent(agentDir.toURI());
    }

    @Deactivate
    public void deactivateManager() {
        agent.stop();
        // TODO use IOUtils
        FileUtils.deleteAll(agentDir);
    }

    // TODO hide?
    @Override
    public IProvisioningAgent getAgent() {
        return agent;
    }

    @Override
    public PublishingRepository getPublishingRepository(ReactorProjectIdentities project) {
        return new PublishingRepositoryImpl(agent, project);
    }

    @Override
    public TargetPlatform computePreliminaryTargetPlatform(ReactorProject project,
            TargetPlatformConfigurationStub tpConfiguration, ExecutionEnvironmentConfiguration eeConfiguration,
            List<ReactorProject> reactorProjects) {
        //
        // at this point, there is only incomplete ("dependency-only") metadata for the reactor projects
        TargetPlatform result = tpFactory.createTargetPlatform(tpConfiguration, eeConfiguration, reactorProjects);
        project.setContextValue(PRELIMINARY_TARGET_PLATFORM_KEY, result);

        List<MavenArtifactRepositoryReference> repositoryReferences = tpConfiguration.getTargetDefinitions().stream()
                .flatMap(definition -> definition.getLocations().stream()).filter(MavenGAVLocation.class::isInstance)
                .map(MavenGAVLocation.class::cast).flatMap(location -> location.getRepositoryReferences().stream())
                .collect(Collectors.toList());
        project.setContextValue(TychoConstants.CTX_REPOSITORY_REFERENCE, repositoryReferences);
        return result;
    }

    @Override
    public TargetPlatform computeFinalTargetPlatform(ReactorProject project,
            List<? extends ReactorProjectIdentities> upstreamProjects, PomDependencyCollector pomDependencyCollector) {
        PreliminaryTargetPlatformImpl preliminaryTargetPlatform = getRegisteredPreliminaryTargetPlatform(project);
        if (preliminaryTargetPlatform == null) {
            // project doesn't seem to use resolver=p2
            return null;
        }
        List<PublishingRepository> upstreamProjectResults = getBuildResults(upstreamProjects);
        TargetPlatform result = tpFactory.createTargetPlatformWithUpdatedReactorContent(preliminaryTargetPlatform,
                upstreamProjectResults, pomDependencyCollector);

        project.setContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY, result);
        return result;
    }

    private PreliminaryTargetPlatformImpl getRegisteredPreliminaryTargetPlatform(ReactorProject project) {
        Object result = project.getContextValue(PRELIMINARY_TARGET_PLATFORM_KEY);
        if (result instanceof PreliminaryTargetPlatformImpl) {
            return (PreliminaryTargetPlatformImpl) result;
        }
        return null;
    }

    private List<PublishingRepository> getBuildResults(List<? extends ReactorProjectIdentities> projects) {
        List<PublishingRepository> results = new ArrayList<>(projects.size());
        for (ReactorProjectIdentities project : projects) {
            results.add(getPublishingRepository(project));
        }
        return results;
    }

    @Override
    public TargetPlatform getFinalTargetPlatform(ReactorProject project) {
        TargetPlatform targetPlatform = (TargetPlatform) project
                .getContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY);
        if (targetPlatform == null) {
            throw new IllegalStateException("Target platform is missing");
        }
        return targetPlatform;
    }

    // TODO use IOUtils
    private static File createTempDir(String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "");
        tempFile.delete();
        tempFile.mkdirs();
        if (!tempFile.isDirectory()) {
            throw new IOException("Failed to create temporary directory: " + tempFile);
        }
        return tempFile;
    }

}
