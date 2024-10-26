/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich    - [Bug 538144] - Support other target locations (Directory, Features, Installations)
 *                          - [Bug 533747] - Target file is read and parsed over and over again
 *                          - [Bug 568729] - Support new "Maven" Target location
 *                          - [Issue #496] - ResolutionArguments#hashcode is not stable 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.MavenTargetLocationFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Service instance for resolving target definitions. Results are cached so that there is no
 * redundant computations in the common case where all modules have the same target definition file
 * configured.
 */
@Singleton
@Named
public class TargetDefinitionResolverService {

    private static final String CACHE_MISS_MESSAGE = "Target definition content cache miss: ";

    private final ConcurrentMap<ResolutionArguments, CompletableFuture<TargetDefinitionContent>> resolutionCache = new ConcurrentHashMap<>();

    private final MavenContext mavenContext;
    private final MavenTargetLocationFactory dependenciesResolver;
    private final TargetDefinitionVariableResolver varResolver;

    @Inject
    public TargetDefinitionResolverService(MavenContext mavenContext,
                                           MavenTargetLocationFactory dependenciesResolver,
                                           TargetDefinitionVariableResolver varResolver) {
        this.mavenContext = mavenContext;
        this.dependenciesResolver = dependenciesResolver;
        this.varResolver = varResolver;
    }

    public TargetDefinitionContent getTargetDefinitionContent(TargetDefinition definition,
            List<TargetEnvironment> environments, ExecutionEnvironmentResolutionHints jreIUs,
            IncludeSourceMode includeSourceMode, ReferencedRepositoryMode referencedRepositoryMode,
            IProvisioningAgent agent) {
        ResolutionArguments arguments = new ResolutionArguments(definition, environments, jreIUs, includeSourceMode,
                referencedRepositoryMode, agent);
        CompletableFuture<TargetDefinitionContent> future = resolutionCache.computeIfAbsent(arguments,
                this::resolveFromArguments);

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof RuntimeException runtimeEx ? runtimeEx : new RuntimeException(cause);
        }

    }

    // this method must only have the cache key as parameter (to make sure that the key is complete)
    private CompletableFuture<TargetDefinitionContent> resolveFromArguments(ResolutionArguments arguments) {
        mavenContext.getLogger().info("Resolving " + arguments);
        if (mavenContext.getLogger().isDebugEnabled()) {
            debugCacheMiss(arguments);
        }

        TargetDefinitionResolver resolver = new TargetDefinitionResolver(arguments.environments, arguments.jreIUs,
                arguments.includeSourceMode, arguments.referencedRepositoryMode, mavenContext, dependenciesResolver,
                varResolver);
        try {
            return CompletableFuture.completedFuture(resolver.resolveContent(arguments.definition, arguments.agent));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void debugCacheMiss(ResolutionArguments arguments) {
        if (resolutionCache.isEmpty()) {
            return;
        }

        // find cache entries which differ only in one of the arguments
        List<String> fieldsInWhichDistanceOneEntriesDiffer = new ArrayList<>();
        for (ResolutionArguments existingKey : resolutionCache.keySet()) {
            List<String> differingFields = arguments.getNonEqualFields(existingKey);
            if (differingFields.size() == 1) {
                fieldsInWhichDistanceOneEntriesDiffer.add(differingFields.get(0));
            }
        }

        if (fieldsInWhichDistanceOneEntriesDiffer.isEmpty()) {
            mavenContext.getLogger().debug(CACHE_MISS_MESSAGE + "All entries differ in more than one parameter");
        } else {
            mavenContext.getLogger()
                    .debug(CACHE_MISS_MESSAGE
                            + "All entries differ, but there are entries which only differ in one parameter: "
                            + fieldsInWhichDistanceOneEntriesDiffer);
        }
    }

    private static final class ResolutionArguments {

        final TargetDefinition definition;
        final List<TargetEnvironment> environments;
        final ExecutionEnvironmentResolutionHints jreIUs;
        final IProvisioningAgent agent;
        private IncludeSourceMode includeSourceMode;
        private ReferencedRepositoryMode referencedRepositoryMode;

        public ResolutionArguments(TargetDefinition definition, List<TargetEnvironment> environments,
                ExecutionEnvironmentResolutionHints jreIUs, IncludeSourceMode includeSourceMode,
                ReferencedRepositoryMode repositoryMode, IProvisioningAgent agent) {
            this.definition = definition;
            this.environments = environments;
            this.jreIUs = jreIUs;
            this.includeSourceMode = includeSourceMode;
            this.referencedRepositoryMode = repositoryMode;
            this.agent = agent;
        }

        @Override
        public int hashCode() {
            return Objects.hash(agent, definition, environments, jreIUs, includeSourceMode, referencedRepositoryMode);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || //
                    (obj instanceof ResolutionArguments other && Objects.equals(jreIUs, other.jreIUs) //
                            && Objects.equals(definition, other.definition) //
                            && Objects.equals(agent, other.agent) // expected to be object identity
                            && Objects.equals(environments, other.environments) //
                            && Objects.equals(includeSourceMode, other.includeSourceMode)
                            && Objects.equals(referencedRepositoryMode, other.referencedRepositoryMode));
        }

        public List<String> getNonEqualFields(ResolutionArguments other) {
            List<String> result = new ArrayList<>();
            addIfNonEqual(result, "target definition", definition, other.definition);
            addIfNonEqual(result, "execution environment", jreIUs, other.jreIUs);
            addIfNonEqual(result, "target environments", environments, other.environments);
            addIfNonEqual(result, "remote p2 repository options", agent, other.agent);
            addIfNonEqual(result, "include source mode", includeSourceMode, other.includeSourceMode);
            addIfNonEqual(result, "include reference mode", referencedRepositoryMode, other.referencedRepositoryMode);
            return result;
        }

        @Override
        public String toString() {
            return "target definition " + definition.getOrigin() + " for environments=" + environments
                    + ", include source mode=" + includeSourceMode + ", referenced repository mode ="
                    + referencedRepositoryMode + ", execution environment=" + jreIUs + ", remote p2 repository options="
                    + agent;
        }

    }

    static <T> void addIfNonEqual(List<String> result, String stringToAdd, T left, T right) {
        if (!Objects.equals(left, right)) {
            result.add(stringToAdd);
        }
    }
}
