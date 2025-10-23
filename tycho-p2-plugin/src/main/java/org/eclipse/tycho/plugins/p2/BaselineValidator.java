/*******************************************************************************
 * Copyright (c) 2012, 2025 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.core.EcJLogFileEnhancer;
import org.eclipse.tycho.p2.metadata.IP2Artifact;

public interface BaselineValidator {

    Map<String, IP2Artifact> validateAndReplace(MavenProject project, ComparisonData data,
            Map<String, IP2Artifact> reactorMetadata, List<Repository> baselineRepositories, BaselineMode baselineMode,
            BaselineReplace baselineReplace, EcJLogFileEnhancer enhancer) throws IOException, MojoExecutionException;

}
