/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;

public interface DependencyResolverConfiguration {

    List<Dependency> getExtraRequirements();

    OptionalResolutionAction getOptionalResolutionAction();

    /**
     * @return Environments to consider for resolver. If null, resolver will use all environments
     *         supported by targetPlatform
     */
    List<TargetEnvironment> getEnvironments();

}
