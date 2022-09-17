/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #845 - Feature restrictions are not taken into account when using emptyVersion
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.Feature.ImportRef;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;

@Component(role = FeatureXmlTransformer.class)
public class FeatureXmlTransformer {
	private static final int KBYTE = 1024;

	@Requirement
	private Logger log;

	@Requirement
	private FileLockService fileLockService;

	public FeatureXmlTransformer() {
	}

	public FeatureXmlTransformer(Logger log, FileLockService fileLockService) {
		this.log = log;
		this.fileLockService = fileLockService;
	}

	/**
	 * Replaces references in the feature model with versions from the target
	 * platform.
	 * 
	 * @param feature The feature model to have plug-in and feature references
	 *                completed.
	 */
	public Feature expandReferences(Feature feature, TargetPlatform targetPlatform) throws MojoFailureException {

		Map<String, ImportRef> featureImports = feature.getRequires().stream().flatMap(req -> req.getImports().stream())
				.filter(imp -> Objects.nonNull(imp.getFeature()) && !imp.getFeature().isBlank())
				.collect(Collectors.toMap(ImportRef::getFeature, Function.identity(),
						checkDuplicates(ImportRef::getFeature, "feature")));
		Map<String, ImportRef> pluginImports = feature.getRequires().stream().flatMap(req -> req.getImports().stream())
				.filter(imp -> Objects.nonNull(imp.getPlugin()) && !imp.getPlugin().isBlank()).collect(Collectors.toMap(
						ImportRef::getPlugin, Function.identity(), checkDuplicates(ImportRef::getPlugin, "plugin")));

		for (PluginRef pluginRef : feature.getPlugins()) {
			String version = pluginRef.getVersion();
			if (Version.emptyVersion.toString().equals(version)) {
				ImportRef importRef = pluginImports.get(pluginRef.getId());
				if (isVersionedRef(importRef)) {
					version = String.format("%s|%s", importRef.getVersion(), importRef.getMatch());
				}
			}
			ArtifactKey plugin = resolvePluginReference(targetPlatform, pluginRef, version);
			pluginRef.setVersion(plugin.getVersion());

			File location = targetPlatform.getArtifactLocation(plugin);
			if (location == null) {
				throw new MojoFailureException("location is missing for plugin " + plugin);
			}
			setDownloadAndInstallSize(pluginRef, location);
		}

		for (FeatureRef featureRef : feature.getIncludedFeatures()) {
			String version = featureRef.getVersion();
			if (Version.emptyVersion.toString().equals(version)) {
				ImportRef importRef = featureImports.get(featureRef.getId());
				if (isVersionedRef(importRef)) {
					version = String.format("%s|%s", importRef.getVersion(), importRef.getMatch());
				}
			}
			ArtifactKey includedFeature = resolveFeatureReference(targetPlatform, featureRef, version);
			featureRef.setVersion(includedFeature.getVersion());
		}

		return feature;
	}

	private static BinaryOperator<ImportRef> checkDuplicates(Function<ImportRef, String> id, String name) {
		return (a, b) -> {
			throw new RuntimeException("duplicate reference to " + name + " " + id.apply(a) + " encountered: "
					+ System.lineSeparator() + a + System.lineSeparator() + b);
		};
	}

	private boolean isVersionedRef(ImportRef importRef) {
		if (importRef == null) {
			return false;
		}
		String version = importRef.getVersion();
		if (version == null || version.isEmpty()) {
			return false;
		}
		String match = importRef.getMatch();
		if (match == null || match.isEmpty()) {
			return false;
		}
		return true;
	}

	private ArtifactKey resolvePluginReference(TargetPlatform targetPlatform, PluginRef pluginRef, String version)
			throws MojoFailureException {
		try {
			return targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, pluginRef.getId(), version);
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Invalid plugin reference with id=" + quote(pluginRef.getId())
					+ " and version=" + quote(pluginRef.getVersion()) + ": " + e.getMessage(), e);
		}
	}

	private ArtifactKey resolveFeatureReference(TargetPlatform targetPlatform, FeatureRef featureRef, String version)
			throws MojoFailureException {
		try {
			return targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE, featureRef.getId(), version);
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Invalid feature reference with id=" + quote(featureRef.getId())
					+ " and version " + quote(featureRef.getVersion()) + ": " + e.getMessage(), e);
		}
	}

	private static String quote(String nullableString) {
		if (nullableString == null)
			return null;
		else
			return "\"" + nullableString + "\"";
	}

	private void setDownloadAndInstallSize(PluginRef pluginRefToEdit, File artifact) {
		// TODO 375111 optionally disable this?
		long downloadSize = 0;
		long installSize = 0;
		if (artifact.isFile()) {
			installSize = getInstallSize(artifact);
			downloadSize = artifact.length();
		} else {
			log.info("Download/install size is not calculated for directory based bundle " + pluginRefToEdit.getId());
		}

		pluginRefToEdit.setDownloadSize(downloadSize / KBYTE);
		pluginRefToEdit.setInstallSize(installSize / KBYTE);
	}

	protected long getInstallSize(File location) {
		long installSize = 0;
		FileLocker locker = fileLockService.getFileLocker(location);
		locker.lock();
		try {
			try {
				try (JarFile jar = new JarFile(location)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						long entrySize = entry.getSize();
						if (entrySize > 0) {
							installSize += entrySize;
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not determine installation size of file " + location, e);
			}
		} finally {
			locker.release();
		}
		return installSize;
	}
}
