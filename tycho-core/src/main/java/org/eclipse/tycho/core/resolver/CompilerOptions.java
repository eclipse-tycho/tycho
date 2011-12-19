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

public class CompilerOptions implements DependencyResolverConfiguration {

    private final List<Dependency> extraRequirements;

    public CompilerOptions(List<Dependency> extraRequirements) {
        this.extraRequirements = extraRequirements;
    }

    public List<Dependency> getExtraRequirements() {
        if (extraRequirements == null)
            return Collections.emptyList();
        return extraRequirements;
    }

}
