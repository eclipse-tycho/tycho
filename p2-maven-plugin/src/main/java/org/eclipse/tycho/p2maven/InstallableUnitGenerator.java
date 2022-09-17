/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.p2maven.actions.AuthoredIUAction;
import org.eclipse.tycho.p2maven.actions.CategoryDependenciesAction;
import org.eclipse.tycho.p2maven.actions.FeatureDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductFile2;
import org.eclipse.tycho.p2maven.helper.PluginRealmHelper;
import org.eclipse.tycho.p2maven.io.MetadataIO;
import org.xml.sax.SAXException;

/**
 * Component used to generate {@link IInstallableUnit}s from other artifacts
 *
 */
@Component(role = InstallableUnitGenerator.class)
public class InstallableUnitGenerator {

	private static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump")
			|| Boolean.getBoolean("tycho.p2.dump.units");

	@Requirement
	private Logger log;

	private static final String KEY_UNITS = "InstallableUnitGenerator.units";

	@Requirement
	private IProvisioningAgent provisioningAgent;

	@Requirement(role = InstallableUnitProvider.class)
	private Map<String, InstallableUnitProvider> additionalUnitProviders;

	@Requirement
	private PluginRealmHelper pluginRealmHelper;

	@Requirement
	private InstallableUnitPublisher publisher;

	@Requirement
	private PlexusContainer plexus;

	/**
	 * Computes the {@link IInstallableUnit}s for a collection of projects.
	 * 
	 * @param projects the projects to compute InstallableUnits for
	 * @return a map from the passed project to the InstallebalUnits
	 * @throws CoreException if computation for any project failed
	 */
	public Map<MavenProject, Collection<IInstallableUnit>> getInstallableUnits(Collection<MavenProject> projects,
			MavenSession session)
			throws CoreException {
		init();
		Objects.requireNonNull(session);
		List<CoreException> errors = new CopyOnWriteArrayList<CoreException>();
		Map<MavenProject, Collection<IInstallableUnit>> result = new ConcurrentHashMap<MavenProject, Collection<IInstallableUnit>>();
		projects.parallelStream().unordered().takeWhile(nil -> errors.isEmpty()).forEach(project -> {
			try {
				result.put(project, getInstallableUnits(project, session, false));
			} catch (CoreException e) {
				errors.add(e);
			}

		});
		if (errors.isEmpty()) {
			return result;
		}
		if (errors.size() == 1) {
			throw errors.get(0);
		}
		MultiStatus multiStatus = new MultiStatus(InstallableUnitGenerator.class, IStatus.ERROR,
				"computing installable unit units failed");
		errors.forEach(e -> multiStatus.add(e.getStatus()));
		throw new CoreException(multiStatus);
	}

	private void init() {
		// this requirement is here to bootstrap P2 service access
		// see https://github.com/eclipse-equinox/p2/issues/100
		// then this would not be required anymore
		provisioningAgent.getService(IArtifactRepositoryManager.class);
	}

	/**
	 * Computes the {@link IInstallableUnit}s for the given project, the computation
	 * is cached unless forceUpdate is <code>true</code> meaning data is always
	 * regenerated from scratch.
	 * 
	 * @param project     the project to examine
	 * @param session
	 * @param forceUpdate if cached data is fine
	 * @return a (possibly empty) collection of {@link IInstallableUnit}s for the
	 *         given {@link MavenProject}
	 * @throws CoreException if anything goes wrong
	 */
	@SuppressWarnings("unchecked")
	public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session,
			boolean forceUpdate)
			throws CoreException {
		init();
		Objects.requireNonNull(session);
		log.debug("Computing installable units for " + project + ", force update = " + forceUpdate);
		synchronized (project) {
			if (!forceUpdate) {
				Object contextValue = project.getContextValue(KEY_UNITS);
				if (contextValue instanceof Collection<?>) {
					Collection<IInstallableUnit> collection = (Collection<IInstallableUnit>) contextValue;
					if (isCompatible(collection)) {
						log.debug("Using cached value for " + project);
						return collection;
					} else {
						log.debug("Can't use cached value for " + project
								+ " because of incompatible classloaders, update is forced!");
					}
				}
			}
			List<IPublisherAction> actions = new ArrayList<>();
			File basedir = project.getBasedir();
			if (basedir == null || !basedir.isDirectory()) {
				log.warn("No valid basedir for " + project + "!");
				return Collections.emptyList();
			}
			String packaging = project.getPackaging();
			switch (packaging) {
			case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
			case PackagingType.TYPE_ECLIPSE_PLUGIN: {
				actions.add(new BundlesAction(new File[] { basedir }));
				break;
			}
			case PackagingType.TYPE_ECLIPSE_FEATURE: {
				FeatureParser parser = new FeatureParser();
				Feature feature = parser.parse(basedir);
				feature.setLocation(basedir.getAbsolutePath());
				FeatureDependenciesAction action = new FeatureDependenciesAction(feature);
				actions.add(action);
				break;
			}
			case PackagingType.TYPE_ECLIPSE_REPOSITORY: {
				File categoryFile = new File(basedir, "category.xml");
				if (categoryFile.exists()) {
					try (InputStream stream = new FileInputStream(categoryFile)) {
						SiteModel siteModel = new CategoryParser(null).parse(stream);
						actions.add(new CategoryDependenciesAction(siteModel, project.getArtifactId(),
								project.getVersion()));
					} catch (IOException | SAXException e) {
						throw new CoreException(Status.error("Error reading " + categoryFile.getAbsolutePath()));
					}
				}
				for (File f : basedir.listFiles(File::isFile)) {
					if (f.getName().endsWith(".product") && !f.getName().startsWith(".polyglot")) {
						try {
							IProductDescriptor productDescriptor = new ProductFile2(f.getAbsolutePath());
							actions.add(new ProductDependenciesAction(productDescriptor));
						} catch (CoreException e) {
							throw e;
						} catch (Exception e) {
							throw new CoreException(Status.error("Error reading " + f.getAbsolutePath() + ": " + e, e));
						}
					}
				}
				break;
			}
			case PackagingType.TYPE_P2_IU: {
				actions.add(new AuthoredIUAction(basedir));
				break;
			}
			default:
			}
			Collection<IInstallableUnit> publishedUnits = publisher.publishMetadata(actions);
			for (InstallableUnitProvider unitProvider : getProvider(project, session)) {
				log.debug("Asking: " + unitProvider + " for additional units for " + project + "...");
				Collection<IInstallableUnit> installableUnits = unitProvider.getInstallableUnits(project, session);
				log.debug("Provider " + unitProvider + " generated " + installableUnits.size() + " (" + installableUnits
						+ ") units for " + project);
				publishedUnits.addAll(installableUnits);
			}
			Collection<IInstallableUnit> result = Collections.unmodifiableCollection(publishedUnits);
			if (DUMP_DATA) {
				File file = new File(project.getBasedir(), "project-units.xml");
				try {
					new MetadataIO().writeXML(result, file);
				} catch (IOException e) {
				}
			}
			if (result.isEmpty()) {
				log.debug("Can't generate any InstallableUnit for packaging type '" + packaging + "' for " + project);
			}
			project.setContextValue(KEY_UNITS, result);
			return result;

		}
	}

	private Collection<InstallableUnitProvider> getProvider(MavenProject project, MavenSession mavenSession)
			throws CoreException {
		Set<InstallableUnitProvider> unitProviders = new HashSet<InstallableUnitProvider>(
				additionalUnitProviders.values());
		try {
			pluginRealmHelper.execute(mavenSession, project, () -> {
				try {
					for (InstallableUnitProvider provider : plexus.lookupList(InstallableUnitProvider.class)) {
						unitProviders.add(provider);
					}
				} catch (ComponentLookupException e) {
					// ignore, nothing was found...
				}
			}, InstallableUnitGenerator::hasPluginDependency);
		} catch (Exception e) {
			throw new CoreException(Status.error("Can't lookup InstallableUnitProviders", e));
		}
		return unitProviders;
	}

	private static boolean isCompatible(Collection<?> collection) {
		// TODO currently causes errors if called from different classloaders!
		// Check how we properly export p2 artifacts to the build!
		if (collection.isEmpty()) {
			return true;
		}
		for (Object unit : collection) {
			if (!IInstallableUnit.class.isInstance(unit)) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasPluginDependency(PluginDescriptor pluginDescriptor) {
		if (pluginDescriptor.getArtifactMap().containsKey(P2Plugin.KEY)) {
			return true;
		}
		return pluginDescriptor.getDependencies().stream().filter(dep -> P2Plugin.GROUP_ID.equals(dep.getGroupId()))
				.filter(dep -> P2Plugin.ARTIFACT_ID.equals(dep.getArtifactId())).findAny().isPresent();
	}

}
