/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;

public class TargetDefinitionResolverService {

    private Map<ResolutionArguments, TargetPlatformContent> resolutionCache = new HashMap<TargetDefinitionResolverService.ResolutionArguments, TargetPlatformContent>();

    // (static) collaborator
    private MavenLogger logger;

    // constructor for DS
    public TargetDefinitionResolverService() {
    }

    // constructor for tests
    public TargetDefinitionResolverService(MavenContext mavenContext) {
        this.logger = mavenContext.getLogger();
    }

    public TargetPlatformContent getTargetDefinitionContent(TargetDefinition definition,
            List<Map<String, String>> environments, JREInstallableUnits jreIUs, IProvisioningAgent agent) {
        ResolutionArguments arguments = new ResolutionArguments(definition, environments, jreIUs, agent);
        TargetPlatformContent resolution = getTargetDefinitionContent(arguments);
        return resolution;
    }

    private TargetPlatformContent getTargetDefinitionContent(ResolutionArguments arguments) {
        TargetPlatformContent resolution = resolutionCache.get(arguments);

        if (resolution == null) {
            resolution = resolveFromArguments(arguments);
            resolutionCache.put(arguments, resolution);
        }
        return resolution;
    }

    // this method must only have the cache key as parameter (to make sure that the key is complete)
    private TargetPlatformContent resolveFromArguments(ResolutionArguments arguments) {
        return new TargetDefinitionResolver(arguments.environments, arguments.jreIUs, arguments.agent, logger)
                .resolveContent(arguments.definition);
    }

    // setter for DS
    public void setMavenContext(MavenContext mavenContext) {
        this.logger = mavenContext.getLogger();
    }

    private static final class ResolutionArguments {

        final TargetDefinition definition;
        final List<Map<String, String>> environments;
        final JREInstallableUnits jreIUs;
        final IProvisioningAgent agent;

        public ResolutionArguments(TargetDefinition definition, List<Map<String, String>> environments,
                JREInstallableUnits jreIUs, IProvisioningAgent agent) {
            this.definition = definition;
            this.environments = environments;
            this.jreIUs = jreIUs;
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
                    && eq(environments, other.environments);
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
}
