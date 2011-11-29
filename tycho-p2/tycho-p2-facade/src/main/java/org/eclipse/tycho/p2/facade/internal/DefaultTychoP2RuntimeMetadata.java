/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.core.utils.TychoVersion;

@Component(role = TychoP2RuntimeMetadata.class, hint = TychoP2RuntimeMetadata.HINT_FRAMEWORK)
public class DefaultTychoP2RuntimeMetadata implements TychoP2RuntimeMetadata {
    private static final List<Dependency> ARTIFACTS;

    static {
        ARTIFACTS = new ArrayList<Dependency>();

        String p2Version = TychoVersion.getTychoVersion();

        ARTIFACTS.add(newDependency("org.eclipse.tycho", "tycho-bundles-external", p2Version, "zip"));
        ARTIFACTS.add(newDependency("org.eclipse.tycho", "org.eclipse.tycho.p2.resolver.impl", p2Version, "jar"));
        ARTIFACTS.add(newDependency("org.eclipse.tycho", "org.eclipse.tycho.p2.maven.repository", p2Version, "jar"));
        ARTIFACTS.add(newDependency("org.eclipse.tycho", "org.eclipse.tycho.p2.tools.impl", p2Version, "jar"));
    }

    public List<Dependency> getRuntimeArtifacts() {
        return ARTIFACTS;
    }

    private static Dependency newDependency(String groupId, String artifactId, String version, String type) {
        Dependency d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion(version);
        d.setType(type);
        return d;
    }

}
