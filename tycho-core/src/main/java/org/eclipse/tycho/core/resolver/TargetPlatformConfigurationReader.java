/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich -  [Bug 461284] - Improve discovery and attach of .target files in eclipse-target-definition
 *                          [Bug 567098] - pomDependencies=consider should wrap non-osgi jars
 *                          [Issue 792]  - Support exclusion of certain dependencies from pom dependency consideration 
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;

public interface TargetPlatformConfigurationReader {
    String TARGET_DEFINITION_INCLUDE_SOURCE = "targetDefinitionIncludeSource";
    String REFERENCED_REPOSITORY_MODE = "referencedRepositoryMode";
    String DEPENDENCY_RESOLUTION = "dependency-resolution";
    String OPTIONAL_DEPENDENCIES = "optionalDependencies";
    String LOCAL_ARTIFACTS = "localArtifacts";
    String LOCAL_ARTIFACTS_PROPERTY = "tycho.localArtifacts";

    String FILTERS = "filters";
    String RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS = "resolveWithExecutionEnvironmentConstraints";
    String REQUIRE_EAGER_RESOLVE = "requireEagerResolve";
    String PROPERTY_REQUIRE_EAGER_RESOLVE = "tycho.target.eager";
    String PROPERTY_ALIAS_REQUIRE_EAGER_RESOLVE = "tycho.resolver.classic";
    String BREE_HEADER_SELECTION_POLICY = "breeHeaderSelectionPolicy";
    String EXECUTION_ENVIRONMENT_DEFAULT = "executionEnvironmentDefault";
    String EXECUTION_ENVIRONMENT = "executionEnvironment";
    String POM_DEPENDENCIES = "pomDependencies";
    String PROPERTY_POM_DEPENDENCIES = "tycho.target.pomDependencies";
    String TARGET = "target";
    String RESOLVER = "resolver";
    String ENVIRONMENTS = "environments";
    String EXCLUSIONS = "exclusions";

    TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project)
            throws TargetPlatformConfigurationException;
}
