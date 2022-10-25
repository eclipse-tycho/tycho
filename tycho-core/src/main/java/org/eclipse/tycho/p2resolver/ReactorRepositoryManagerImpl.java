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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
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
import org.eclipse.tycho.p2.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

@Component(role = ReactorRepositoryManager.class)
public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager, Disposable {

    private static final String PRELIMINARY_TARGET_PLATFORM_KEY = ReactorRepositoryManagerImpl.class.getName()
            + "/dependencyOnlyTargetPlatform";

    @Requirement
    private IProvisioningAgentProvider agentFactory;

    @Requirement
    P2ResolverFactory p2ResolverFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    private TargetPlatformFactory tpFactory;

    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    public void bindP2ResolverFactory(P2ResolverFactory p2ResolverFactory) {
        this.p2ResolverFactory = p2ResolverFactory;
    }

    @Override
    public void dispose() {
        if (agent != null) {
            agent.stop();
        }
        if (agentDir != null) {
            // TODO use IOUtils
            FileUtils.deleteAll(agentDir);
        }
    }

    // TODO hide?
    @Override
    public synchronized IProvisioningAgent getAgent() {
        if (agent == null) {
            try {
                agentDir = createTempDir("tycho_reactor_agent");
                agent = agentFactory.createAgent(agentDir.toURI());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ProvisionException e) {
                throw new RuntimeException(e);
            }
        }
        return agent;
    }

    @Override
    public PublishingRepository getPublishingRepository(ReactorProjectIdentities project) {
        return new PublishingRepositoryImpl(getAgent(), project);
    }

    @Override
    public TargetPlatform computePreliminaryTargetPlatform(ReactorProject project,
            TargetPlatformConfigurationStub tpConfiguration, ExecutionEnvironmentConfiguration eeConfiguration,
            List<ReactorProject> reactorProjects) {
        //
        // at this point, there is only incomplete ("dependency-only") metadata for the reactor projects
        TargetPlatform result = getTpFactory().createTargetPlatform(tpConfiguration, eeConfiguration, reactorProjects);
        project.setContextValue(PRELIMINARY_TARGET_PLATFORM_KEY, result);

        List<MavenArtifactRepositoryReference> repositoryReferences = tpConfiguration.getTargetDefinitions().stream()
                .flatMap(definition -> definition.getLocations().stream()).filter(MavenGAVLocation.class::isInstance)
                .map(MavenGAVLocation.class::cast).flatMap(location -> location.getRepositoryReferences().stream())
                .toList();
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
        TargetPlatform result = getTpFactory().createTargetPlatformWithUpdatedReactorContent(preliminaryTargetPlatform,
                upstreamProjectResults, pomDependencyCollector);

        project.setContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY, result);
        return result;
    }

    private PreliminaryTargetPlatformImpl getRegisteredPreliminaryTargetPlatform(ReactorProject project) {
        return project.getContextValue(
                PRELIMINARY_TARGET_PLATFORM_KEY) instanceof PreliminaryTargetPlatformImpl preliminaryTargetPlatformImpl
                        ? preliminaryTargetPlatformImpl
                        : null;
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

    public synchronized TargetPlatformFactory getTpFactory() {
        if (tpFactory == null) {
            tpFactory = p2ResolverFactory.getTargetPlatformFactory();
        }
        return tpFactory;
    }

}
