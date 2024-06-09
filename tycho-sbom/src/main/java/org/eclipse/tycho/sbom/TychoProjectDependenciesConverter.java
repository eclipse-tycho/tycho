/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.sbom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.cyclonedx.maven.DefaultProjectDependenciesConverter;
import org.cyclonedx.maven.ProjectDependenciesConverter;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.tools.P2DependencyTreeGenerator;
import org.eclipse.tycho.p2.tools.P2DependencyTreeGenerator.DependencyTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.codehaus.plexus.component.annotations.Component(role = ProjectDependenciesConverter.class)
public class TychoProjectDependenciesConverter extends DefaultProjectDependenciesConverter {
	private static final Logger LOG = LoggerFactory.getLogger(TychoProjectDependenciesConverter.class);

	@Inject
	private LegacySupport legacySupport;

	@Inject
	private TychoModelConverter modelConverter;

	@Inject
	private P2DependencyTreeGenerator dependencyGenerator;

	private final Map<IInstallableUnit, List<String>> bomRepresentations = new ConcurrentHashMap<>();

	@Override
	public void cleanupBomDependencies(Metadata metadata, Map<String, Component> components,
			Map<String, Dependency> dependencies) {
		final MavenSession mavenSession = legacySupport.getSession();
		final MavenProject currentProject = mavenSession.getCurrentProject();
		//
		try {
			Set<IInstallableUnit> unmapped = new HashSet<>();
			List<DependencyTreeNode> rootNodes = dependencyGenerator.buildDependencyTree(currentProject, unmapped);
			// Something doesn't seem right...
			if (rootNodes.isEmpty() && unmapped.isEmpty()) {
				LOG.info("Project " + currentProject + " doesn't seem to be a Tycho project. Skip...");
				return;
			}
			// Synchronize with dependency tree node
			TreeSet<Dependency> newDependencies = new TreeSet<>(Comparator.comparing(Dependency::getRef));
			for (DependencyTreeNode rootNode : rootNodes) {
				convertToDependency(rootNode, newDependencies);
			}
			for (IInstallableUnit iu : unmapped) {
				for (String bomRef : getBomRepresentation(iu)) {
					newDependencies.add(new Dependency(bomRef));
				}
			}
			for (Dependency dependency : newDependencies) {
				dependencies.put(dependency.getRef(), dependency);
			}
		} catch (CoreException e) {
			LOG.error(e.getMessage());
		}
	}

	private void convertToDependency(DependencyTreeNode node, Set<Dependency> dependencies) {
		for (String bomRef : getBomRepresentation(node.getInstallableUnit())) {
			Dependency dependency = new Dependency(bomRef);
			for (DependencyTreeNode childNode : node.getChildren()) {
				for (String childBomRef : getBomRepresentation(childNode.getInstallableUnit())) {
					dependency.addDependency(new Dependency(childBomRef));
				}
				convertToDependency(childNode, dependencies);
			}
			dependencies.add(dependency);
		}
	}

	/**
	 * Calculates the BOM representation of the give {@link IInstallableUnit}. We
	 * need to distinguish between IUs that are part of the current reactor build
	 * and IUs external IUs, in order to properly handle e.g. the version qualifier.
	 * For reactor IUs, the BOM is calculated using the Maven artifact, otherwise
	 * via the {@link IArtifactKey}s. If no BOM representation can be calculated, an
	 * empty list is returned.
	 * 
	 * @param iu The installable unit for which to generate
	 * @return An {@code immutable} list of all bom representations matching the
	 *         given IU.
	 */
	private List<String> getBomRepresentation(IInstallableUnit iu) {
		return bomRepresentations.computeIfAbsent(iu, ignore -> {
			final MavenSession mavenSession = legacySupport.getSession();
			final List<MavenProject> reactorProjects = mavenSession.getAllProjects();
			// (I) IU describes local reactor project
			for (MavenProject project : reactorProjects) {
				ReactorProject reactorProject = DefaultReactorProject.adapt(project);
				Set<IInstallableUnit> initalUnits = reactorProject.getDependencyMetadata(DependencyMetadataType.INITIAL);
				Set<IInstallableUnit> seedUnits = reactorProject.getDependencyMetadata(DependencyMetadataType.SEED);
				if (initalUnits.contains(iu) || seedUnits.contains(iu)) {
					String bomRef = modelConverter.generatePackageUrl(project.getArtifact());
					if (bomRef == null) {
						LOG.error("Unable to calculate BOM for: " + project);
						return Collections.emptyList();
					}
					return Collections.singletonList(bomRef);
				}
			}
			// (II) IU describes external artifact
			final List<String> bomRefs = new ArrayList<>();
			for (IArtifactKey p2artifactKey : iu.getArtifacts()) {
				String bomRef = modelConverter.generateP2PackageUrl(p2artifactKey, true, true, false);
				if (bomRef == null) {
					LOG.error("Unable to calculate BOM for: " + p2artifactKey);
					continue;
				}
				bomRefs.add(bomRef);
			}
			return Collections.unmodifiableList(bomRefs);
		});
	}
}
