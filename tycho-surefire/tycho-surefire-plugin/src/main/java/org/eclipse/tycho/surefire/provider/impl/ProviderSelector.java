/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Component(role = ProviderSelector.class)
public class ProviderSelector {

    @Requirement
    private Map<String, TestFrameworkProvider> providers;

    private static final Comparator<TestFrameworkProvider> VERSION_COMPARATOR = new Comparator<TestFrameworkProvider>() {

        public int compare(TestFrameworkProvider p1, TestFrameworkProvider p2) {
            return p1.getVersion().compareTo(p2.getVersion());
        }
    };

    public TestFrameworkProvider selectProvider(List<ClasspathEntry> classpath, Properties providerProperties,
            String providerHint) throws MojoExecutionException {
        if (providerHint != null) {
            TestFrameworkProvider provider = providers.get(providerHint);
            if (provider == null) {
                throw new MojoExecutionException("Could not find test framework provider with role hint '"
                        + providerHint + "'. Available providers: " + providers.keySet());
            } else {
                return provider;
            }
        }
        List<TestFrameworkProvider> candidates = new ArrayList<TestFrameworkProvider>();
        for (TestFrameworkProvider provider : providers.values()) {
            if (provider.isEnabled(classpath, providerProperties)) {
                candidates.add(provider);
            }
        }
        validateCandidates(candidates);
        TestFrameworkProvider highestVersionProvider = Collections.max(candidates, VERSION_COMPARATOR);
        validate(highestVersionProvider, providerProperties);
        return highestVersionProvider;
    }

    private void validate(TestFrameworkProvider provider, Properties providerProperties) throws MojoExecutionException {
        if ("junit".equals(provider.getType()) && providerProperties.getProperty("parallel") != null) {
            if (!JUnit47Provider.class.equals(provider.getClass())) {
                throw new MojoExecutionException("Provider " + provider.getClass().getName()
                        + " does not support parallel mode. " + JUnit47Provider.class.getName()
                        + " (i.e. JUnit >= 4.7) is required for this.");
            }
        }
    }

    public Set<Artifact> filterTestFrameworkBundles(TestFrameworkProvider provider, List<Artifact> pluginArtifacts)
            throws MojoExecutionException {
        Set<Artifact> result = new LinkedHashSet<Artifact>();
        List<org.sonatype.aether.artifact.Artifact> requiredArtifacts = new ArrayList<org.sonatype.aether.artifact.Artifact>();
        requiredArtifacts.add(new DefaultArtifact("org.eclipse.tycho", "org.eclipse.tycho.surefire.osgibooter", null,
                null));
        requiredArtifacts.addAll(provider.getRequiredBundles());
        for (org.sonatype.aether.artifact.Artifact dependency : requiredArtifacts) {
            boolean found = false;
            for (Artifact artifact : pluginArtifacts) {
                if (dependency.getGroupId().equals(artifact.getGroupId())) {
                    if (dependency.getArtifactId().equals(artifact.getArtifactId())) {
                        if (dependency.getVersion() != null && !"".equals(dependency.getVersion())) {
                            if (dependency.getVersion().equals(artifact.getVersion())) {
                                found = true;
                                result.add(artifact);
                                break;
                            }
                        } else {
                            found = true;
                            result.add(artifact);
                            break;
                        }
                    }
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

    private void validateCandidates(List<TestFrameworkProvider> candidates) throws MojoExecutionException {
        if (candidates.size() == 0) {
            throw new MojoExecutionException("Could not determine test framework provider. Available providers: "
                    + providers.keySet());
        } else if (candidates.size() == 1) {
            return;
        }
        // candidates.size() > 1
        final String firstType = candidates.get(0).getType();
        for (int i = 1; i < candidates.size(); i++) {
            if (!firstType.equals(candidates.get(i).getType())) {
                throw new MojoExecutionException(
                        "Could not determine test framework provider. Providers with different types (" + firstType
                                + "," + candidates.get(i).getType()
                                + ") are enabled. Try specifying a providerHint; available provider hints: "
                                + providers.keySet());
            }
        }
    }

}
