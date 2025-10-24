/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.buildversion;

import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import javax.inject.Inject;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * <p>
 * This mojo calculates build timestamp as the latest timestamp of the project
 * itself and timestamps of bundles and features directly included in the
 * project. This is meant to work with custom timestamp providers and generate
 * build qualifier based on build contents, i.e. the source code, and not the
 * time the build was started; rebuilding the same source code will result in
 * the same version qualifier.
 * </p>
 * <p>
 * Timestamp of included bundles and features is determined by parsing their
 * respective version qualifiers. Qualifiers that cannot be parsed are silently
 * ignored, which can result in old version qualifier used even when aggregator
 * project contents actually changed. In this case aggregator project timestamp
 * will have to be increased manually, using artificial SCM commit for example.
 * </p>
 * <p>
 * Qualifier aggregation is enabled only for projects with custom timestamp
 * provider, i.e. &lt;timestampProvider&gt; is set in pom.xml to a value other
 * than "default". The default build timestamp provider uses build start time as
 * build timestamp, which should be newer or equal than timestamp of any
 * included bundle/feature project, which makes qualifier aggregation redundant.
 * </p>
 */
@Mojo(name = "build-qualifier-aggregator", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BuildQualifierAggregatorMojo extends BuildQualifierMojo {

	@Inject
	private TimestampFinder timestampFinder;

	@Inject
	private TargetPlatformService platformService;

	@Override
	protected Date getBuildTimestamp() throws MojoExecutionException {
		Date timestamp = super.getBuildTimestamp();

		if (timestampProvider == null) {
			// default timestamp is essentially this build start time
			// no included bundle/feature can have more recent timestamp
			return timestamp;
		}

		final Date[] latestTimestamp = new Date[] { timestamp };

		TychoProject projectType = projectTypes.get(project.getPackaging());
		if (projectType == null) {
			throw new IllegalStateException("Unknown or unsupported packaging type " + packaging);
		}

		final ReactorProject thisProject = DefaultReactorProject.adapt(project);
		// TODO we need to trigger TP loading now, probably we also should better use
		// the target platform instead of walker of the project type?
		platformService.getTargetPlatform(thisProject);

		projectType.getDependencyWalker(thisProject).walk(new ArtifactDependencyVisitor() {
			@Override
			public boolean visitFeature(FeatureDescription feature) {
				if (feature.getFeatureRef() == null || thisProject.equals(feature.getMavenProject())) {
					// 'this' feature
					return true; // visit immediately included features
				}
				visitArtifact(feature);
				return false; // do not visit indirectly included features/bundles
			}

			@Override
			public void visitPlugin(PluginDescription plugin) {
				if (plugin.getPluginRef() == null || thisProject.equals(plugin.getMavenProject())) {
					// 'this' bundle
					return;
				}
				visitArtifact(plugin);
			}

			private void visitArtifact(ArtifactDescriptor artifact) {

				Date timestamp = timestampFinder.findByDescriptor(artifact, format);
				if (timestamp != null && latestTimestamp[0].compareTo(timestamp) < 0) {
					latestTimestamp[0] = timestamp;
				}

			}

		});

		return latestTimestamp[0];
	}
}
