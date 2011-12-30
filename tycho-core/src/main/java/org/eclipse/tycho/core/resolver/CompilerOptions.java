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
package org.eclipse.tycho.core.resolver;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;

public class CompilerOptions implements DependencyResolverConfiguration {

    private OptionalResolutionAction optionalAction;
    private final List<Dependency> extraRequirements;

    public CompilerOptions(OptionalResolutionAction optionalAction, List<Dependency> extraRequirements) {
        if (optionalAction == null)
            throw new NullPointerException();

        this.optionalAction = optionalAction;
        this.extraRequirements = extraRequirements;
    }

    /**
     * @return never <code>null</code>
     */
    public OptionalResolutionAction getOptionalResolutionAction() {
        return optionalAction;
    }

    /**
     * @return never <code>null</code>
     */
    public List<Dependency> getExtraRequirements() {
        if (extraRequirements == null)
            return Collections.emptyList();
        return extraRequirements;
    }

}
