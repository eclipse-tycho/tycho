/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.util.List;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;

public abstract class ArtifactDependencyVisitor {
    public boolean visitFeature(FeatureDescription feature) {
        return true; // keep visiting
    }

    public void visitPlugin(PluginDescription plugin) {

    }

    public void missingFeature(FeatureRef ref, List<ArtifactDescriptor> walkback) {
        throw newRuntimeException("Could not resolve feature", ref.toString(), walkback);
    }

    public void missingPlugin(PluginRef ref, List<ArtifactDescriptor> walkback) {
        throw newRuntimeException("Could not resolve plugin", ref.toString(), walkback);
    }

    protected RuntimeException newRuntimeException(String message, String missing, List<ArtifactDescriptor> walkback) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append(" ").append(missing).append("; Path to dependency:\n");
        for (ArtifactDescriptor artifact : walkback) {
            sb.append("  ").append(artifact.toString()).append("\n");
        }
        return new RuntimeException(sb.toString());
    }
}
