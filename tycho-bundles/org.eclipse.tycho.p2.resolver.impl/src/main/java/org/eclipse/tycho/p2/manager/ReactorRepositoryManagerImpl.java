/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TychoParameters;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.P2TargetPlatform;
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
    private static final String FINAL_TARGET_PLATFORM_KEY = TargetPlatform.FINAL_TARGET_PLATFORM_KEY;

    private MavenContext mavenContext;

    private IProvisioningAgentProvider agentFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    private TargetPlatformFactory tpFactory;

    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    public void bindP2ResolverFactory(P2ResolverFactory p2ResolverFactory) {
        this.tpFactory = p2ResolverFactory.getTargetPlatformFactory();
    }

    public void bindMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void activateManager() throws IOException, ProvisionException {
        agentDir = createTempDir("tycho_reactor_agent");
        agent = agentFactory.createAgent(agentDir.toURI());
    }

    public void deactivateManager() {
        agent.stop();
        FileUtils.deleteQuietly(agentDir);
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
            List<ReactorProject> reactorProjects, PomDependencyCollector pomDependencies) {
        // at this point, there is only incomplete ("dependency-only") metadata for the reactor projects
        TargetPlatform result = tpFactory.createTargetPlatform(tpConfiguration, eeConfiguration, reactorProjects,
                pomDependencies, new PreliminaryTargetPlatformLogConfig(project.getBasedir()));
        project.setContextValue(PRELIMINARY_TARGET_PLATFORM_KEY, result);
        return result;
    }

    @Override
    public void computeFinalTargetPlatform(ReactorProject project,
            List<? extends ReactorProjectIdentities> upstreamProjects) {
        PreliminaryTargetPlatformImpl preliminaryTargetPlatform = getRegisteredPreliminaryTargetPlatform(project);
        if (preliminaryTargetPlatform == null) {
            // project doesn't seem to use resolver=p2
            return;
        }

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
        List<PublishingRepository> results = new ArrayList<>(projects.size());
        for (ReactorProjectIdentities project : projects) {
            results.add(getPublishingRepository(project));
        }
        return results;
    }

    @Override
    public TargetPlatform getFinalTargetPlatform(ReactorProject project) {
        TargetPlatform targetPlatform = (TargetPlatform) project.getContextValue(FINAL_TARGET_PLATFORM_KEY);
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

    private class PreliminaryTargetPlatformLogConfig implements TargetPlatformFactory.LogConfiguration {

        private final File projectRoot;

        public PreliminaryTargetPlatformLogConfig(File projectRoot) {
            this.projectRoot = projectRoot;
        }

        @Override
        public boolean diskLoggingEnabled() {
            // TODO use getUserProperties - option only makes sense on the command line
            return Boolean
                    .valueOf(mavenContext.getSessionProperties().getProperty(TychoParameters.DEBUG_PRELIMINARY_TP));
        }

        @Override
        public String getFilePrefix() {
            return projectRoot + "/tycho.debug.preliminaryTP";
        }

    }

}
