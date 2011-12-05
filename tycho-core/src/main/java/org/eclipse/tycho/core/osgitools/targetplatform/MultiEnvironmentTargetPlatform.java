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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetEnvironment;

// TODO 364134 revise this class
public class MultiEnvironmentTargetPlatform extends DefaultTargetPlatform {
    public Map<TargetEnvironment, DependencyArtifacts> platforms = new LinkedHashMap<TargetEnvironment, DependencyArtifacts>();

    public void addPlatform(TargetEnvironment environment, DefaultTargetPlatform platform) {
        platforms.put(environment, platform);

        artifacts.putAll(platform.artifacts);
        locations.putAll(platform.locations);
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
}
