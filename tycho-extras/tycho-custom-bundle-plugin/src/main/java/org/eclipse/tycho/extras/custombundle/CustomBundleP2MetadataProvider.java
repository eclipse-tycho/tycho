/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.extras.custombundle;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.p2.facade.internal.AttachedArtifact;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

/**
 * This component is invoked during Tycho dependency resolution and provides P2
 * metadata that describes artifacts that will be created by custom-bundle goal.
 */
@Component(role = P2MetadataProvider.class, hint = "org.eclipse.tycho.extras.custombundle.CustomBundleP2MetadataProvider")
public class CustomBundleP2MetadataProvider implements P2MetadataProvider, Initializable {
	@Requirement(hint = TychoServiceFactory.HINT)
	private EquinoxServiceFactory equinox;

	private DependencyMetadataGenerator generator;

	@Override
	public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
			List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
		Map<String, IDependencyMetadata> metadata = new LinkedHashMap<>();
		getCustomArtifacts(project).forEach(artifact -> {
			metadata.put(artifact.getClassifier(), new SecondaryDependencyMetadata(
					generator.generateMetadata(artifact, environments, optionalAction, new PublisherOptions())));
		});
		return metadata;
	}

	static Stream<AttachedArtifact> getCustomArtifacts(MavenProject project) {
		Plugin plugin = project.getPlugin("org.eclipse.tycho.extras:tycho-custom-bundle-plugin");
		if (plugin != null) {
			return plugin.getExecutions().stream().map(execution -> {
				File location = getBundleLocation(execution);
				String classifier = getClassifier(execution);
				if (location != null && classifier != null) {
					return new AttachedArtifact(project, location, classifier);
				}
				return null;
			}).filter(Objects::nonNull);
		}
		return Stream.empty();
	}

	private static String getClassifier(PluginExecution execution) {
		Xpp3Dom cfg = (Xpp3Dom) execution.getConfiguration();
		if (cfg == null) {
			return null;
		}
		Xpp3Dom classifierDom = cfg.getChild("classifier");
		if (classifierDom == null) {
			return null;
		}
		return classifierDom.getValue();
	}

	private static File getBundleLocation(PluginExecution execution) {
		Xpp3Dom cfg = (Xpp3Dom) execution.getConfiguration();
		if (cfg == null) {
			return null;
		}
		Xpp3Dom locationDom = cfg.getChild("bundleLocation");
		if (locationDom == null) {
			return null;
		}
		return new File(locationDom.getValue());
	}

	@Override
	public void initialize() throws InitializationException {
		this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
	}

	private static class SecondaryDependencyMetadata implements IDependencyMetadata {
		final Set<IInstallableUnit> metadata;

		public SecondaryDependencyMetadata(IDependencyMetadata original) {
			metadata = Collections.unmodifiableSet(original.getDependencyMetadata());
		}

		@Override
		public Set<IInstallableUnit> getDependencyMetadata() {
			return metadata;
		}

		@Override
		public Set<IInstallableUnit> getDependencyMetadata(DependencyMetadataType type) {
			return type == DependencyMetadataType.RESOLVE ? metadata : Collections.emptySet();
		}

		@Override
		public void setDependencyMetadata(DependencyMetadataType type, Collection<IInstallableUnit> units) {
			throw new UnsupportedOperationException();
		}
	}
}
