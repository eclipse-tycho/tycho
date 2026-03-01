/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sonatype Inc. - adopted to work outside of eclipse workspace
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.List;
import java.util.function.Function;

import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Helper interface that computes compile dependencies of a bundle project.
 */
public interface DependencyComputer {

    /**
     * Entry representing a dependency with access rules.
     */
    public interface DependencyEntry {
        BundleRevision getRevision();
        java.io.File getLocation();
        boolean isSystemBundle();
        String getSymbolicName();
        org.osgi.framework.Version getVersion();
        ArtifactDescriptor getArtifactDescriptor();
        java.util.Collection<AccessRule> getRules();
    }

    /**
     * Computes and returns the List of dependencies of the given {@link ModuleRevision}.
     * 
     * @param module
     *            the ModuleRevision whose dependencies are computed
     * @param descriptorLookup
     *            function to look up artifact descriptors
     * @return the list of dependencies of the module
     */
    List<DependencyEntry> computeDependencies(ModuleRevision module,
            Function<BundleRevision, ArtifactDescriptor> descriptorLookup);

    /**
     * Computes and returns extra access rules for boot classpath.
     * 
     * @param container
     *            the module container
     * @return the list of access rules
     */
    List<AccessRule> computeBootClasspathExtraAccessRules(ModuleContainer container);

}
