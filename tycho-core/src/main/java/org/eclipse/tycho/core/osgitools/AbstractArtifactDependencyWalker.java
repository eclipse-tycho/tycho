/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.UpdateSite;

public abstract class AbstractArtifactDependencyWalker implements ArtifactDependencyWalker {

    private final DependencyArtifacts artifacts;

    private final TargetEnvironment[] environments;

    public AbstractArtifactDependencyWalker(DependencyArtifacts artifacts) {
        this(artifacts, null);
    }

    public AbstractArtifactDependencyWalker(DependencyArtifacts artifacts, TargetEnvironment[] environments) {
        this.artifacts = artifacts;
        this.environments = environments;
    }

    @Override
    public void traverseUpdateSite(UpdateSite site, ArtifactDependencyVisitor visitor) {
        WalkbackPath visited = new WalkbackPath();

        for (FeatureRef ref : site.getFeatures()) {
            traverseFeature(ref, visitor, visited);
        }
    }

    @Override
    public void traverseFeature(File location, Feature feature, ArtifactDependencyVisitor visitor) {
        traverseFeature(location, feature, null, visitor, new WalkbackPath());
    }

    protected void traverseFeature(File location, Feature feature, FeatureRef featureRef,
            ArtifactDependencyVisitor visitor, WalkbackPath visited) {
        ArtifactDescriptor artifact = getArtifact(location, feature.getId());

        if (artifact == null) {
            // ah?
            throw new IllegalStateException("Feature " + location + " with id " + feature.getId()
                    + " is not part of the project build target platform");
        }

        ArtifactKey key = artifact.getKey();
        ReactorProject project = artifact.getMavenProject();
        String classifier = artifact.getClassifier();
        Set<Object> installableUnits = artifact.getInstallableUnits();

        DefaultFeatureDescription description = new DefaultFeatureDescription(key, location, project, classifier,
                feature, featureRef, installableUnits);

        if (visitor.visitFeature(description)) {
            for (PluginRef ref : feature.getPlugins()) {
                traversePlugin(ref, visitor, visited);
            }

            for (FeatureRef ref : feature.getIncludedFeatures()) {
                traverseFeature(ref, visitor, visited);
            }
        }
    }

    protected ArtifactDescriptor getArtifact(File location, String id) {
        Map<String, ArtifactDescriptor> artifacts = this.artifacts.getArtifact(location);
        if (artifacts != null) {
            for (ArtifactDescriptor artifact : artifacts.values()) {
                if (id.equals(artifact.getKey().getId())) {
                    return artifact;
                }
            }
        }
        return null;
    }

    @Override
    public void traverseProduct(ProductConfiguration product, ArtifactDependencyVisitor visitor) {
        traverseProduct(product, visitor, new WalkbackPath());
    }

    protected void traverseProduct(ProductConfiguration product, ArtifactDependencyVisitor visitor,
            WalkbackPath visited) {
        if (product.useFeatures()) {
            for (FeatureRef ref : product.getFeatures()) {
                traverseFeature(ref, visitor, visited);
            }
        } else {
            for (PluginRef ref : product.getPlugins()) {
                traversePlugin(ref, visitor, visited);
            }
        }

        Set<String> bundles = new HashSet<>();
        for (ArtifactDescriptor artifact : visited.getVisited()) {
            ArtifactKey key = artifact.getKey();
            if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(key.getType())) {
                bundles.add(key.getId());
            }
        }

        if (environments != null && product.includeLaunchers()) {
            for (TargetEnvironment environment : environments) {
                String os = environment.getOs();
                String ws = environment.getWs();
                String arch = environment.getArch();

                String id;

                // for Mac OS X there is no org.eclipse.equinox.launcher.carbon.macosx.x86 or org.eclipse.equinox.launcher.carbon.macosx.ppc folder,
                // only a org.eclipse.equinox.launcher.carbon.macosx folder.
                // see https://jira.codehaus.org/browse/MNGECLIPSE-1075
                if (PlatformPropertiesUtils.OS_MACOSX.equals(os) && (PlatformPropertiesUtils.ARCH_X86.equals(arch)
                        || PlatformPropertiesUtils.ARCH_PPC.equals(arch))) {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os;
                } else {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch;
                }

                if (!bundles.contains(id)) {
                    PluginRef ref = new PluginRef("plugin");
                    ref.setId(id);
                    ref.setOs(os);
                    ref.setWs(ws);
                    ref.setArch(arch);
                    ref.setUnpack(true);
                    traversePlugin(ref, visitor, visited);
                }
            }
        }
    }

    protected void traverseFeature(FeatureRef ref, ArtifactDependencyVisitor visitor, WalkbackPath visited) {
        ArtifactDescriptor artifact = artifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE, ref.getId(),
                ref.getVersion());

        if (artifact != null) {
            if (visited.visited(artifact.getKey())) {
                return;
            }

            visited.enter(artifact);
            try {
                File location = artifact.getLocation(true);

                Feature feature = Feature.loadFeature(location);
                traverseFeature(location, feature, ref, visitor, visited);
            } finally {
                visited.leave(artifact);
            }
        } else {
            visitor.missingFeature(ref, visited.getWalkback());
        }
    }

    private void traversePlugin(PluginRef ref, ArtifactDependencyVisitor visitor, WalkbackPath visited) {
        if (!matchTargetEnvironment(ref)) {
            return;
        }

        ArtifactDescriptor artifact = artifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, ref.getId(),
                ref.getVersion());

        if (artifact != null) {
            ArtifactKey key = artifact.getKey();
            if (visited.visited(key)) {
                return;
            }

            File location = artifact.getLocation(true);
            ReactorProject project = artifact.getMavenProject();
            String classifier = artifact.getClassifier();
            Set<Object> installableUnits = artifact.getInstallableUnits();

            PluginDescription description = new DefaultPluginDescription(key, location, project, classifier, ref,
                    installableUnits);
            visited.enter(description);
            try {
                visitor.visitPlugin(description);
            } finally {
                visited.leave(description);
            }
        } else {
            visitor.missingPlugin(ref, visited.getWalkback());
        }
    }

    private boolean matchTargetEnvironment(PluginRef pluginRef) {
        String pluginOs = pluginRef.getOs();
        String pluginWs = pluginRef.getWs();
        String pluginArch = pluginRef.getArch();

        if (environments == null) {
            // match all environments be default
            return true;

            // no target environments, only include environment independent plugins
            // return pluginOs == null && pluginWs == null && pluginArch == null;
        }

        for (TargetEnvironment environment : environments) {
            if (environment.match(pluginOs, pluginWs, pluginArch)) {
                return true;
            }
        }

        return false;
    }

    protected static class WalkbackPath {
        private Map<ArtifactKey, ArtifactDescriptor> visited = new HashMap<>();

        private Stack<ArtifactDescriptor> walkback = new Stack<>();

        boolean visited(ArtifactKey key) {
            return visited.containsKey(key);
        }

        public List<ArtifactDescriptor> getWalkback() {
            return new ArrayList<>(walkback);
        }

        void enter(ArtifactDescriptor artifact) {
            visited.put(artifact.getKey(), artifact);
            walkback.push(artifact);
        }

        void leave(ArtifactDescriptor artifact) {
            walkback.pop();
        }

        Collection<ArtifactDescriptor> getVisited() {
            return visited.values();
        }
    }
}
