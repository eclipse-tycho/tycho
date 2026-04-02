/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.eclipse.tycho.targetplatform.TargetDefinition.MavenDependency;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.DependencyDepth;

public class OverridePreconditionChecker {

    private static final String CONST_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    /**
     * Validates preconditions for using override instructions on a bundle.
     *
     * @param originalSymbolicName
     *            the bundle symbolic name from the original manifest; may be {@code null} if the
     *            artifact is not a bundle
     * @param instructionsMap
     *            map of instruction sets; must contain exactly one entry with a symbolic name
     *            different from {@code originalSymbolicName}
     * @param dependencyDepth
     *            the dependency depth; must be {@link DependencyDepth#NONE}
     * @param roots
     *            the root dependencies of the current location
     * @return an error message if any precondition fails, otherwise {@code null}
     */
    public static String checkOverridePreconditions(String originalSymbolicName,
            Map<String, Properties> instructionsMap, DependencyDepth dependencyDepth,
            Collection<MavenDependency> roots) {
        if (dependencyDepth != DependencyDepth.NONE) {
            return "The dependency depth must be none!";
        }
        if (originalSymbolicName == null) {
            return "The artifact is no bundle.";
        }
        if (roots.size() != 1) {
            return "The location must contain exactly one root dependency.";
        }
        if (instructionsMap.size() != 1) {
            return "The location must contain exactly one bnd instruction which must contain a symbolic name that differs from the original one.";
        }
        Properties properties = instructionsMap.values().iterator().next();
        if (!isSymbolicNameDefinedAndDiffers(properties, originalSymbolicName)) {
            return " The symbolic name in the bnd instructions must be defined and differ from the original one.";
        }
        return null;
    }

    private static boolean isSymbolicNameDefinedAndDiffers(Properties properties, String originalSymbolicName) {
        if (properties == null) {
            return false;
        }
        Object definedSymbolicName = properties.get(CONST_BUNDLE_SYMBOLIC_NAME);
        return definedSymbolicName != null && !definedSymbolicName.equals(originalSymbolicName);
    }

}
