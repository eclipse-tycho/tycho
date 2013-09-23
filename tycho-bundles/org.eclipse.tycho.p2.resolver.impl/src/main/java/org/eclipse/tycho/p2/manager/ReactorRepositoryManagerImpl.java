/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.PreliminaryTargetPlatformImpl;
import org.eclipse.tycho.p2.target.TargetPlatformFactoryImpl;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    private static final String PRELIMINARY_TARGET_PLATFORM_KEY = ReactorRepositoryManagerImpl.class.getName()
            + "/dependencyOnlyTargetPlatform";
    // see org.eclipse.tycho.core.TychoConstants.CTX_TARGET_PLATFORM // TODO delete the value in TychoConstants
    private static final String FINAL_TARGET_PLATFORM_KEY = "org.eclipse.tycho.core.TychoConstants/targetPlatform";

    private IProvisioningAgentProvider agentFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    private TargetPlatformFactory tpFactory;

    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    public void bindP2ResolverFactory(P2ResolverFactory p2ResolverFactory) {
        tpFactory = p2ResolverFactory.getTargetPlatformFactory();
    }

    public void activateManager() throws IOException, ProvisionException {
        agentDir = createTempDir("tycho_reactor_agent");
        agent = agentFactory.createAgent(agentDir.toURI());
    }

    public void deactivateManager() {
        agent.stop();
        // TODO use IOUtils
        FileUtils.deleteAll(agentDir);
    }

    // TODO hide?
    public IProvisioningAgent getAgent() {
        return agent;
    }

    public PublishingRepository getPublishingRepository(ReactorProjectIdentities project) {
        return new PublishingRepositoryImpl(agent, project);
    }

    public TargetPlatform computePreliminaryTargetPlatform(ReactorProject project,
            TargetPlatformConfigurationStub tpConfiguration, ExecutionEnvironmentConfiguration eeConfiguration,
            List<ReactorProject> reactorProjects, PomDependencyCollector pomDependencies) {
        // at this point, there is only incomplete ("dependency-only") metadata for the reactor projects
        TargetPlatform result = tpFactory.createTargetPlatform(tpConfiguration, eeConfiguration, reactorProjects,
                pomDependencies);
        project.setContextValue(PRELIMINARY_TARGET_PLATFORM_KEY, result);
        return result;
    }

    public void computeFinalTargetPlatform(ReactorProject project,
            List<? extends ReactorProjectIdentities> upstreamProjects) {
        PreliminaryTargetPlatformImpl preliminaryTargetPlatform = getRegisteredPreliminaryTargetPlatform(project);
        if (preliminaryTargetPlatform == null) {
            // TODO 412416 log
            return;
        }

        // TODO 412416 introduce interface on OSGi class loader side to avoid this cast
        List<PublishingRepository> upstreamProjectResults = getBuildResults(upstreamProjects);
        P2TargetPlatform result = ((TargetPlatformFactoryImpl) tpFactory)
                .createTargetPlatformWithUpdatedReactorContent(preliminaryTargetPlatform, upstreamProjectResults);

        project.setContextValue(FINAL_TARGET_PLATFORM_KEY, result);
    }

    private PreliminaryTargetPlatformImpl getRegisteredPreliminaryTargetPlatform(ReactorProject project) {
        Object result = project.getContextValue(PRELIMINARY_TARGET_PLATFORM_KEY);
        if (result instanceof PreliminaryTargetPlatformImpl) {
            return (PreliminaryTargetPlatformImpl) result;
        }
        return null;
    }

    private List<PublishingRepository> getBuildResults(List<? extends ReactorProjectIdentities> projects) {
        List<PublishingRepository> results = new ArrayList<PublishingRepository>(projects.size());
        for (ReactorProjectIdentities project : projects) {
            results.add(getPublishingRepository(project));
        }
        return results;
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
