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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
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
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.p2maven.actions.AuthoredIUAction;
import org.eclipse.tycho.p2maven.actions.CategoryDependenciesAction;
import org.eclipse.tycho.p2maven.actions.FeatureDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductFile2;
import org.eclipse.tycho.p2maven.io.MetadataIO;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.resolver.InstallableUnitProvider;
import org.osgi.framework.Constants;
import org.xml.sax.SAXException;

/**
 * Component used to generate {@link IInstallableUnit}s from other artifacts
 *
 */
@Named
@Singleton
public class InstallableUnitGenerator {

	private static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump")
			|| Boolean.getBoolean("tycho.p2.dump.units");

	@Inject
	private Logger log;

	private static final String KEY_UNITS = "InstallableUnitGenerator.units";

	private static final String KEY_ARTIFACT_FILE = "InstallableUnitGenerator.artifactFile";

	@Inject
	private IProvisioningAgent provisioningAgent;

	@Inject
	private Map<String, InstallableUnitProvider> additionalUnitProviders;

	@Inject
	private PluginRealmHelper pluginRealmHelper;

	@Inject
	private InstallableUnitPublisher publisher;

	@Inject
	private PlexusContainer plexus;

	@Inject
	ArtifactHandlerManager artifactHandlerManager;

	private Map<Artifact, ArtifactUnits> artifactUnitMap = new ConcurrentHashMap<>();

	/**
	 * Computes the {@link IInstallableUnit}s for a collection of projects.
	 * 
	 * @param projects the projects to compute InstallableUnits for
	 * @return a map from the passed project to the InstallebalUnits
	 * @throws CoreException if computation for any project failed
	 */
	public Map<MavenProject, Collection<IInstallableUnit>> getInstallableUnits(Collection<MavenProject> projects,
			MavenSession session) throws CoreException {
		init();
		Objects.requireNonNull(session);
		List<CoreException> errors = new CopyOnWriteArrayList<>();
		Map<MavenProject, Collection<IInstallableUnit>> result = new ConcurrentHashMap<>();
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
			boolean forceUpdate) throws CoreException {
		init();
		Objects.requireNonNull(session);
		log.debug("Computing installable units for " + project + ", force update = " + forceUpdate);
		synchronized (project) {
			File basedir = project.getBasedir();
			if (basedir == null || !basedir.isDirectory()) {
				log.warn("No valid basedir for " + project + " found");
				return Collections.emptyList();
			}
			File projectArtifact = getProjectArtifact(project);
			if (!forceUpdate) {
				// first check if the packed state might has changed...
				if (Objects.equals(project.getContextValue(KEY_ARTIFACT_FILE), projectArtifact)) {
					Object contextValue = project.getContextValue(KEY_UNITS);
					if (contextValue instanceof Collection<?>) {
						// now check if we are classlaoder compatible...
						Collection<IInstallableUnit> collection = (Collection<IInstallableUnit>) contextValue;
						if (isCompatible(collection)) {
							log.debug("Using cached value for " + project);
							return collection;
						} else {
							log.debug("Cannot use cached value for " + project
									+ " because of incompatible classloaders, update is forced");
						}
					}
				} else {
					log.info("Cannot use cached value for " + project
							+ " because project artifact has changed, update is forced");
				}
			}
			String packaging = project.getPackaging();
			String version = project.getVersion();
			String artifactId = project.getArtifactId();
			List<IPublisherAction> actions = getPublisherActions(packaging, basedir, projectArtifact, version,
					artifactId);
			Collection<IInstallableUnit> publishedUnits = publisher.publishMetadata(actions);
			for (InstallableUnitProvider unitProvider : getProvider(project, session)) {
				log.debug("Asking " + unitProvider + " for additional units for " + project);
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
				log.debug("Cannot generate any InstallableUnit for packaging type '" + packaging + "' for " + project);
			}
			project.setContextValue(KEY_UNITS, result);
			project.setContextValue(KEY_ARTIFACT_FILE, projectArtifact);
			return result;

		}
	}

	private static File getProjectArtifact(MavenProject project) {
		Artifact artifact = project.getArtifact();
		if (artifact != null) {
			File file = artifact.getFile();
			if (file != null && file.exists()) {
				return file;
			}
		}
		return null;
	}

	private List<IPublisherAction> getPublisherActions(String packaging, File basedir, File projectArtifact,
			String version, String artifactId) throws CoreException {
		List<IPublisherAction> actions = new ArrayList<>();
		switch (packaging) {
		case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
		case PackagingType.TYPE_ECLIPSE_PLUGIN: {
			File bundleFile = Objects.requireNonNullElse(projectArtifact, basedir);
			actions.add(new BundlesAction(new File[] { bundleFile }));
			break;
		}
		case PackagingType.TYPE_ECLIPSE_FEATURE: {
			FeatureParser parser = new FeatureParser();
			File featureFile = Objects.requireNonNullElse(projectArtifact, basedir);
			Feature feature = parser.parse(featureFile);
			feature.setLocation(featureFile.getAbsolutePath());
			FeatureDependenciesAction action = new FeatureDependenciesAction(feature);
			actions.add(action);
			break;
		}
		case PackagingType.TYPE_ECLIPSE_REPOSITORY: {
			File categoryFile = new File(basedir, "category.xml");
			if (categoryFile.exists()) {
				try (InputStream stream = new FileInputStream(categoryFile)) {
					SiteModel siteModel = new CategoryParser(null).parse(stream);
					actions.add(new CategoryDependenciesAction(siteModel, artifactId, version));
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
		return actions;
	}

	public Collection<IInstallableUnit> getInstallableUnits(IProductDescriptor productDescriptor) throws CoreException {
		return publisher.publishMetadata(List.of(new ProductDependenciesAction(productDescriptor)));
	}

	public Collection<IInstallableUnit> getInstallableUnits(Manifest manifest) {
		Attributes mainAttributes = manifest.getMainAttributes();
		CaseInsensitiveDictionaryMap<String, String> headers = new CaseInsensitiveDictionaryMap<>(
				mainAttributes.size());
		Set<Entry<Object, Object>> entrySet = mainAttributes.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			headers.put(entry.getKey().toString(), entry.getValue().toString());
		}
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
		BundleDescription bundleDescription = BundlesAction.createBundleDescription(headers, null);
		IInstallableUnit iu = BundlesAction.createBundleIU(bundleDescription, BundlesAction.createBundleArtifactKey(
				bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString()), publisherInfo);
		return List.of(iu);
	}

	public Collection<IInstallableUnit> getInstallableUnits(Artifact artifact) {
		return artifactUnitMap.computeIfAbsent(artifact, x -> new ArtifactUnits()).getUnits(artifact);
	}

	/**
	 * Compute the additional provided units for a ReactorProject
	 * 
	 * @param reactorProject
	 * @return a collection of units for the given reactor project
	 */
	public Collection<IInstallableUnit> getProvidedInstallableUnits(ReactorProject reactorProject) {
		MavenProject mavenProject = reactorProject.adapt(MavenProject.class);
		MavenSession mavenSession = reactorProject.adapt(MavenSession.class);
		try {
			return getProvider(mavenProject, mavenSession).stream().flatMap(provider -> {
				try {
					return provider.getInstallableUnits(mavenProject, mavenSession).stream();
				} catch (CoreException e) {
					return Stream.empty();
				}
			}).toList();
		} catch (CoreException e) {
			return List.of();
		}
	}

	private Collection<InstallableUnitProvider> getProvider(MavenProject project, MavenSession mavenSession)
			throws CoreException {
		Set<InstallableUnitProvider> unitProviders = new HashSet<>(additionalUnitProviders.values());
		try {
			pluginRealmHelper.visitPluginExtensions(project, mavenSession, InstallableUnitProvider.class,
					unitProviders::add);
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

	private final class ArtifactUnits {

		private Collection<IInstallableUnit> units;
		private long lastModified;

		public synchronized Collection<IInstallableUnit> getUnits(Artifact artifact) {
			if (units != null && !hasChanges(artifact)) {
				return units;
			}
			try {
				// TODO in case of "java-source" type, we might want to generate the source IU
				// based on the parent artifact!
				File file = artifact.getFile();
				if (isValidFile(file)) {
					lastModified = file.lastModified();
					String type = artifact.getType();
					if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(type)
							|| PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(type) || "bundle".equals(type)) {
						List<IPublisherAction> actions = getPublisherActions(PackagingType.TYPE_ECLIPSE_PLUGIN, file,
								file, artifact.getVersion(), artifact.getArtifactId());
						return units = publisher.publishMetadata(actions);
					} else if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(type)) {
						List<IPublisherAction> actions = getPublisherActions(PackagingType.TYPE_ECLIPSE_FEATURE, file,
								file, artifact.getVersion(), artifact.getArtifactId());
						return units = publisher.publishMetadata(actions);
					} else {
						boolean isBundle = false;
						boolean isFeature = false;
						try (JarFile jarFile = new JarFile(file)) {
							Manifest manifest = jarFile.getManifest();
							isBundle = manifest != null
									&& manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
							isFeature = jarFile.getEntry("feature.xml") != null;
						} catch (IOException e) {
							// can't determine the type then...
						}
						if (isBundle) {
							List<IPublisherAction> actions = getPublisherActions(PackagingType.TYPE_ECLIPSE_PLUGIN,
									file, file, artifact.getVersion(), artifact.getArtifactId());
							return units = publisher.publishMetadata(actions);
						}
						if (isFeature) {
							List<IPublisherAction> actions = getPublisherActions(PackagingType.TYPE_ECLIPSE_FEATURE,
									file, file, artifact.getVersion(), artifact.getArtifactId());
							return units = publisher.publishMetadata(actions);
						}
					}
				}
			} catch (CoreException e) {
				// can't generate one then...
			}
			return units = Collections.emptyList();
		}

		private boolean isValidFile(File file) {
			return file != null && file.getName().toLowerCase().endsWith(".jar") && file.exists();
		}

		private boolean hasChanges(Artifact artifact) {
			File file = artifact.getFile();
			if (isValidFile(file)) {
				return file.lastModified() != lastModified;
			}
			return false;
		}

	}

}
