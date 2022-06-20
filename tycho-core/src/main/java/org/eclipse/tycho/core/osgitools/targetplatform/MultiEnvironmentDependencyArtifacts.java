/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich    - Bug 567782 - Platform specific fragment not support in Multi-Platform POMless build
 *                          - Issue #626 - Classpath computation must take fragments into account 
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.artifacts.DependencyArtifacts;

public class MultiEnvironmentDependencyArtifacts extends DefaultDependencyArtifacts {
    public Map<TargetEnvironment, DependencyArtifacts> platforms = new LinkedHashMap<>();

    public MultiEnvironmentDependencyArtifacts() {
        this(null);
    }

    public MultiEnvironmentDependencyArtifacts(ReactorProject project) {
        super(project);
    }

    public void addPlatform(TargetEnvironment environment, DefaultDependencyArtifacts platform) {
        platforms.put(environment, platform);

        for (ArtifactDescriptor artifact : platform.artifacts.values()) {
            addArtifact(artifact, true);
        }

        nonReactorUnits.addAll(platform.nonReactorUnits);
    }

    public DependencyArtifacts getPlatform(TargetEnvironment environment) {
        return platforms.get(environment);
    }

    @Override
    public void toDebugString(StringBuilder sb, String linePrefix) {
        for (Map.Entry<TargetEnvironment, DependencyArtifacts> entry : platforms.entrySet()) {
            sb.append(linePrefix);
            sb.append("Target environment: ").append(entry.getKey().toString()).append("\n");
            entry.getValue().toDebugString(sb, linePrefix);
        }
    }

    public Collection<TargetEnvironment> getPlatforms() {
        return Collections.unmodifiableCollection(platforms.keySet());
    }

    @Override
    public Collection<ArtifactDescriptor> getFragments() {
        return platforms.values().stream().map(DependencyArtifacts::getFragments).flatMap(Collection::stream).distinct()
                .collect(Collectors.toList());
    }
}
