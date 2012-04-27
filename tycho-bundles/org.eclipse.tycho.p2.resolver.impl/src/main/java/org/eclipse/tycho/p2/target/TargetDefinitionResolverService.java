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

import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;

public class TargetDefinitionResolverService {

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
        return new TargetDefinitionResolver(environments, jreIUs, agent, logger).resolveContent(definition);
    }

    // setter for DS
    public void setMavenContext(MavenContext mavenContext) {
        this.logger = mavenContext.getLogger();
    }

}
