/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = TychoOsgiRuntimeArtifacts.class, hint = TychoOsgiRuntimeArtifacts.HINT_SHARED)
public class TychoOsgiSharedArtifacts implements TychoOsgiRuntimeArtifacts {
    private static final List<Dependency> ARTIFACTS;

    static {
        ARTIFACTS = new ArrayList<>();
    }

    @Override
    public List<Dependency> getRuntimeArtifacts() {
        return Collections.unmodifiableList(ARTIFACTS);
    }

}
