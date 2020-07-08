/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich    - [Bug 538144] Support other target locations (Directory, Features, Installations)
 *                          - [Bug 533747] - Target file is read and parsed over and over again
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

/**
 * Service instance for resolving target definitions. Results are cached so that there is no
 * redundant computations in the common case where all modules have the same target definition file
 * configured.
 */
public class TargetDefinitionResolverService {

    private static final String CACHE_MISS_MESSAGE = "Target definition content cache miss: ";

    private Map<ResolutionArguments, TargetDefinitionContent> resolutionCache = new HashMap<>();

    private MavenContext mavenContext;

    private IProvisioningAgent provisioningAgent;

    // constructor for DS
    public TargetDefinitionResolverService() {
    }

    public TargetDefinitionContent getTargetDefinitionContent(TargetDefinition definition,
            List<TargetEnvironment> environments, ExecutionEnvironmentResolutionHints jreIUs,
            IncludeSourceMode includeSourceMode, IProvisioningAgent agent) {
        this.provisioningAgent = agent;
        ResolutionArguments arguments = new ResolutionArguments(definition, environments, jreIUs, includeSourceMode,
                agent);

        TargetDefinitionContent resolution = resolutionCache.get(arguments);

        if (resolution == null) {
            if (mavenContext.getLogger().isDebugEnabled()) {
                debugCacheMiss(arguments);
                mavenContext.getLogger().debug("Resolving target definition content...");
            }

            resolution = resolveFromArguments(arguments);
            resolutionCache.put(arguments, resolution);
        }
        return resolution;
    }

    // this method must only have the cache key as parameter (to make sure that the key is complete)
    private TargetDefinitionContent resolveFromArguments(ResolutionArguments arguments) {
        return new TargetDefinitionResolver(arguments.environments, arguments.jreIUs, arguments.includeSourceMode,
                mavenContext).resolveContent(arguments.definition, provisioningAgent);
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

    // setter for DS
    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    private static final class ResolutionArguments {

        final TargetDefinition definition;
        final List<TargetEnvironment> environments;
        final ExecutionEnvironmentResolutionHints jreIUs;
        final IProvisioningAgent agent;
        private IncludeSourceMode includeSourceMode;

        public ResolutionArguments(TargetDefinition definition, List<TargetEnvironment> environments,
                ExecutionEnvironmentResolutionHints jreIUs, IncludeSourceMode includeSourceMode,
                IProvisioningAgent agent) {
            this.definition = definition;
            this.environments = environments;
            this.jreIUs = jreIUs;
            this.includeSourceMode = includeSourceMode;
            this.agent = agent;
        }

        @Override
        public int hashCode() {
            final int prime = 61;
            int result = 1;
            result = prime * result + ((agent == null) ? 0 : agent.hashCode());
            result = prime * result + ((definition == null) ? 0 : definition.hashCode());
            result = prime * result + ((environments == null) ? 0 : environments.hashCode());
            result = prime * result + ((jreIUs == null) ? 0 : jreIUs.hashCode());
            result = prime * result + ((includeSourceMode == null) ? 0 : includeSourceMode.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ResolutionArguments))
                return false;

            ResolutionArguments other = (ResolutionArguments) obj;
            return eq(jreIUs, other.jreIUs) //
                    && eq(definition, other.definition) //
                    && eq(agent, other.agent) // expected to be object identity
                    && eq(environments, other.environments) //
                    && eq(includeSourceMode, other.includeSourceMode);
        }

        public List<String> getNonEqualFields(ResolutionArguments other) {
            List<String> result = new ArrayList<>();
            addIfNonEqual(result, "target definition", definition, other.definition);
            addIfNonEqual(result, "execution environment", jreIUs, other.jreIUs);
            addIfNonEqual(result, "target environments", environments, other.environments);
            addIfNonEqual(result, "remote p2 repository options", agent, other.agent);
            addIfNonEqual(result, "include source mode", includeSourceMode, other.includeSourceMode);
            return result;
        }

    }

    static <T> boolean eq(T left, T right) {
        if (left == right) {
            return true;
        } else if (left == null) {
            return false;
        } else {
            return left.equals(right);
        }
    }

    static <T> void addIfNonEqual(List<String> result, String stringToAdd, T left, T right) {
        if (!eq(left, right)) {
            result.add(stringToAdd);
        }
    }
}
