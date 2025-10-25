/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;

@Named
@Singleton
public class DefaultProviderHelper implements ProviderHelper {

    @Inject
    private Map<String, TestFrameworkProvider> providers;

    @Inject
    private BundleReader bundleReader;

    private static final Comparator<ProviderSelection> VERSION_COMPARATOR = Comparator
            .comparing(ProviderSelection::provider, Comparator.comparing(TestFrameworkProvider::getVersion));

    @Override
    public ProviderSelection selectProvider(MavenProject project, List<ClasspathEntry> classpath,
            Properties providerProperties, String providerHint) throws MojoExecutionException {
        if (providerHint != null) {
            TestFrameworkProvider provider = providers.get(providerHint);
            if (provider == null) {
                throw new MojoExecutionException("Could not find test framework provider with role hint '"
                        + providerHint + "'. Available providers: " + providers.keySet());
            } else {
                return new ProviderSelection(provider, providerHint);
            }
        }
        List<ProviderSelection> candidates = new ArrayList<>();
        for (Entry<String, TestFrameworkProvider> provider : providers.entrySet()) {
            if (provider.getValue().isEnabled(project, classpath, providerProperties)) {
                candidates.add(new ProviderSelection(provider.getValue(), provider.getKey()));
            }
        }
        validateCandidates(candidates);
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return Collections.max(candidates, VERSION_COMPARATOR);
    }

    @Override
    public Set<Artifact> filterTestFrameworkBundles(TestFrameworkProvider provider, List<Artifact> pluginArtifacts)
            throws MojoExecutionException {
        Set<Artifact> result = new LinkedHashSet<>();
        List<Dependency> requiredArtifacts = new ArrayList<>();
        requiredArtifacts.add(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.osgibooter"));
        requiredArtifacts.addAll(provider.getRequiredArtifacts());
        for (Dependency dependency : requiredArtifacts) {
            boolean found = false;
            for (Artifact artifact : pluginArtifacts) {
                if (dependency.getGroupId().equals(artifact.getGroupId())
                        && dependency.getArtifactId().equals(artifact.getArtifactId())
                        && (dependency.getVersion() == null || dependency.getVersion().isEmpty()
                                || dependency.getVersion().equals(artifact.getVersion()))) {
                    found = true;
                    result.add(artifact);
                    break;
                }
            }
            if (!found) {
                StringBuilder sb = new StringBuilder("Unable to locate test framework dependency " + dependency + "\n");
                sb.append("Test framework: " + provider.getSurefireProviderClassName() + "\n");
                sb.append("All plugin artifacts: ");
                for (Artifact artifact : pluginArtifacts) {
                    sb.append("\n\t").append(artifact.toString());
                }
                throw new MojoExecutionException(sb.toString());
            }
        }
        return result;
    }

    static Dependency newDependency(String groupId, String artifactId) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        return dependency;
    }

    static Dependency newDependency(String artifactId) {
        return newDependency("org.eclipse.tycho", artifactId);
    }

    private void validateCandidates(List<ProviderSelection> candidates) throws MojoExecutionException {
        if (candidates.isEmpty()) {
            throw new MojoExecutionException(
                    "Could not determine test framework provider. Available providers: " + providers.keySet());
        } else if (candidates.size() == 1) {
            return;
        }
        // candidates.size() > 1
        final String firstType = candidates.get(0).provider().getType();
        for (int i = 1; i < candidates.size(); i++) {
            if (!firstType.equals(candidates.get(i).provider().getType())) {
                throw new MojoExecutionException(
                        "Could not determine test framework provider. Providers with different types (" + firstType
                                + "," + candidates.get(i).provider().getType()
                                + ") are enabled. Try specifying a providerHint; available provider hints: "
                                + providers.keySet());
            }
        }
    }

    @Override
    public List<String> getSymbolicNames(Set<Artifact> bundleArtifacts) {
        List<String> result = new ArrayList<>();
        for (Artifact artifact : bundleArtifacts) {
            result.add(bundleReader.loadManifest(artifact.getFile()).getBundleSymbolicName());
        }
        return result;
    }

}
