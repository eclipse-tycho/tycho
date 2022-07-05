/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.p2maven.actions.AuthoredIUAction;
import org.eclipse.tycho.p2maven.actions.CategoryDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductFile2;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

/**
 * Component used to generate {@link IInstallableUnit}s from other artifacts
 *
 */
@Component(role = InstallableUnitGenerator.class)
public class InstallableUnitGenerator {

	@Requirement
	private Logger log;

	private static final String KEY_UNITS = "InstallableUnitGenerator.units";

	// this requirement is here to bootstrap P2 service access
	@Requirement(hint = "plexus")
	private BundleContext bundleContext;

	/**
	 * Computes the {@link IInstallableUnit}s for a collection of projects.
	 * 
	 * @param projects the projects to compute InstallableUnits for
	 * @return a map from the passed project to the InstallebalUnits
	 * @throws CoreException if computation for any project failed
	 */
	public Map<MavenProject, Collection<IInstallableUnit>> getInstallableUnits(Collection<MavenProject> projects)
			throws CoreException {
		List<CoreException> errors = new CopyOnWriteArrayList<CoreException>();
		Map<MavenProject, Collection<IInstallableUnit>> result = new ConcurrentHashMap<MavenProject, Collection<IInstallableUnit>>();
		projects.parallelStream().unordered().takeWhile(nil -> errors.isEmpty()).forEach(project -> {
			try {
				result.put(project, getInstallableUnits(project, false));
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

	/**
	 * Computes the {@link IInstallableUnit}s for the given project, the computation
	 * is cached unless forceUpdate is <code>true</code> meaning data is always
	 * regenerated from scratch.
	 * 
	 * @param project     the project to examine
	 * @param forceUpdate if cached data is fine
	 * @return a (possibly empty) collection of {@link IInstallableUnit}s for the
	 *         given {@link MavenProject}
	 * @throws CoreException if anything goes wrong
	 */
	@SuppressWarnings("unchecked")
	public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, boolean forceUpdate)
			throws CoreException {
		synchronized (project) {
			if (!forceUpdate) {
				Object contextValue = project.getContextValue(KEY_UNITS);
				if (contextValue instanceof Collection<?>) {
					Collection<IInstallableUnit> collection = (Collection<IInstallableUnit>) contextValue;
					if (isCompatible(collection)) {
						return collection;
					}
				}
			}
			List<IPublisherAction> actions = new ArrayList<>();

			File basedir = project.getBasedir();
			if (basedir == null || !basedir.isDirectory()) {
				return Collections.emptyList();
			}
			switch (project.getPackaging()) {
			case PackagingType.TYPE_ECLIPSE_TEST_PLUGIN:
			case PackagingType.TYPE_ECLIPSE_PLUGIN: {
				actions.add(new BundlesAction(new File[] { basedir }));
				break;
			}
			case PackagingType.TYPE_ECLIPSE_FEATURE: {
				FeatureParser parser = new FeatureParser();
				Feature feature = parser.parse(basedir);
				Map<IInstallableUnit, Feature> featureMap = new HashMap<>();
				FeaturesAction action = new FeaturesAction(new Feature[] { feature }) {
					@Override
					protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU,
							IPublisherInfo publisherInfo) {
						// so not call super as we don't wan't to copy anything --> Bug in P2 with
						// IPublisherInfo.A_INDEX option
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578380
					}

					@Override
					protected IInstallableUnit generateFeatureJarIU(Feature feature, IPublisherInfo publisherInfo) {
						IInstallableUnit iu = super.generateFeatureJarIU(feature, publisherInfo);
						featureMap.put(iu, feature);
						return iu;
					}
				};
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
				log.debug("Can't generate any InstallableUnit for packaging type " + project.getPackaging() + " for "
						+ project);
				return Collections.emptyList();
			}
			if (actions.isEmpty()) {
				List<IInstallableUnit> list = Collections.emptyList();
				project.setContextValue(KEY_UNITS, list);
				return list;
			}
			PublisherInfo publisherInfo = new PublisherInfo();
			publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
			PublisherResult results = new PublisherResult();
			for (IPublisherAction action : actions) {
				IStatus status = action.perform(publisherInfo, results, new NullProgressMonitor());
				if (status.matches(IStatus.ERROR)) {
					throw new CoreException(status);
				}
			}
			Set<IInstallableUnit> result = Collections
					.unmodifiableSet(results.query(QueryUtil.ALL_UNITS, null).toSet());
			project.setContextValue(KEY_UNITS, result);
			return result;

		}
	}

	private boolean isCompatible(Collection<?> collection) {
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

}
