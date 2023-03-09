/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.util.List;
import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.core.TargetPlatformConfiguration.LocalArtifactHandling;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;

public class DependencyResolutionConfiguration {

    public class ExtraRequirementConfiguration {
        // Adapted from DefaultArtifactKey
        public String type;
        public String id;
        public String versionRange;
    }

    public OptionalResolutionAction optionalDependencies;
    public List<ExtraRequirementConfiguration> extraRequirements;
    public Properties profileProperties;
    @Parameter(property = DefaultTargetPlatformConfigurationReader.LOCAL_ARTIFACTS_PROPERTY, name = DefaultTargetPlatformConfigurationReader.LOCAL_ARTIFACTS)
    public LocalArtifactHandling localArtifacts;
}
