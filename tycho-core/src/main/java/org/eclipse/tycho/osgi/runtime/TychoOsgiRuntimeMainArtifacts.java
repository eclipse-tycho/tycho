/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = TychoOsgiRuntimeArtifacts.class, hint = TychoOsgiRuntimeArtifacts.HINT_FRAMEWORK)
public class TychoOsgiRuntimeMainArtifacts implements TychoOsgiRuntimeArtifacts {
    private static final List<Dependency> ARTIFACTS;

    static {
        ARTIFACTS = new ArrayList<>();

        ARTIFACTS.add(
                TychoOsgiRuntimeArtifacts.newDependency("org.eclipse.platform", "org.eclipse.osgi", "3.18.0", "jar"));

    }

    @Override
    public List<Dependency> getRuntimeArtifacts() {
        return ARTIFACTS;
    }

}
