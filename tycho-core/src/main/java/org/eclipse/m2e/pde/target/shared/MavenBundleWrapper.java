/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.shared;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.pde.target.shared.ProcessingMessage.Type;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;

/**
 * The {@link MavenBundleWrapper} handle the hard part of the target support
 * that is wrapping an existing jar into a bundle that is:
 * <ul>
 * <li>Find all dependencies of an artifact</li>
 * <li>For each dependency check if it also needs wrapping</li>
 * <li>Depending on the target and used instructions, the wrapping might be
 * different</li>
 * <li><As wrapping a bundle is a hard task we actually want to cache the data
 * as much as possible/li>
 * <li>The code is generic enough so we can have the exact same implementation
 * at Tycho, e.g. we only use maven, BND and java API but nothing from m2e!</li>
 * </ul>
 */
public class MavenBundleWrapper {
	private MavenBundleWrapper() {
	}

	/**
	 * Wraps an artifact (and possible its dependents if required) to produce a
	 * manifest with OSGi metadata.
	 * 
	 * @param artifact           the artifact to wrap
	 * @param instructionsLookup a lookup for bnd instructions
	 * @param repositories       the repositories that should be used to resolve
	 *                           dependencies
	 * @param repoSystem         the repository system for lookup dependent items
	 * @param repositorySession  the session to use
	 * @param syncContextFactory the sync context factory to acquire exclusive
	 *                           access to the wrapped artifact and its dependencies
	 * @return the wrapped artifact
	 * @throws Exception if wrapping the artifact fails for any reason
	 */
	public static WrappedBundle getWrappedArtifact(Artifact artifact,
			Function<DependencyNode, Properties> instructionsLookup, List<RemoteRepository> repositories,
			RepositorySystem repoSystem, RepositorySystemSession repositorySession,
			SyncContextFactory syncContextFactory) throws Exception {
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, null));
		collectRequest.setRepositories(repositories);
		DependencyNode node = repoSystem.collectDependencies(repositorySession, collectRequest).getRoot();

		DependencyRequest dependencyRequest = new DependencyRequest();
		dependencyRequest.setRoot(node);
		repoSystem.resolveDependencies(repositorySession, dependencyRequest);

		try (SyncContext syncContext = syncContextFactory.newInstance(repositorySession, false)) {
			Set<Artifact> lockList = new HashSet<>();
			node.accept(new DependencyVisitor() {

				@Override
				public boolean visitLeave(DependencyNode n) {
					return true;
				}

				@Override
				public boolean visitEnter(DependencyNode n) {
					lockList.add(n.getArtifact());
					return true;
				}
			});
			syncContext.acquire(lockList, null);
			Map<DependencyNode, WrappedBundle> visited = new HashMap<>();
			WrappedBundle wrappedNode = getWrappedNode(node, instructionsLookup, visited);
			for (WrappedBundle wrap : visited.values()) {
				wrap.getJar().close();
			}
			return wrappedNode;
		}
	}

	private static WrappedBundle getWrappedNode(DependencyNode node,
			Function<DependencyNode, Properties> instructionsLookup, Map<DependencyNode, WrappedBundle> visited)
			throws Exception {
		WrappedBundle wrappedNode = visited.get(node);
		if (wrappedNode != null) {
			return wrappedNode;
		}
		Artifact artifact = node.getArtifact();
		File originalFile = artifact.getFile();
		Jar jar = new Jar(originalFile);
		Manifest originalManifest = jar.getManifest();
		if (originalManifest != null
				&& originalManifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
			// already a bundle!
			visited.put(node,
					wrappedNode = new WrappedBundle(node, List.of(), null, originalFile.toPath(), jar, List.of()));
			return wrappedNode;
		}
		List<DependencyNode> children = node.getChildren();
		List<WrappedBundle> depends = new ArrayList<>();
		for (DependencyNode child : children) {
			depends.add(getWrappedNode(child, instructionsLookup, visited));
		}
		WrappedBundle wrappedNodeAfterVisit = visited.get(node);
		if (wrappedNodeAfterVisit != null) {
			return wrappedNodeAfterVisit;
		}
		Properties instructions = instructionsLookup.apply(node);
		String key = getInstructionsKey(instructions, depends);
		try (Jar analyzerJar = jar) {
			// now we know the key and the depends we enter the critical section of checking
			// if the data is already there or needs to be refreshed
			File parent = new File(originalFile.getParent(), "bnd-" + key);
			File wrapArtifactFile = new File(parent, originalFile.getName());
			Jar cached = getCachedJar(wrapArtifactFile.toPath(), originalFile.toPath());
			if (cached == null) {
				List<ProcessingMessage> messages = new ArrayList<>();
				wrapArtifactFile.getParentFile().mkdirs();
				try (Analyzer analyzer = new Analyzer(analyzerJar);) {
					analyzer.setProperty("mvnGroupId", artifact.getGroupId());
					analyzer.setProperty("mvnArtifactId", artifact.getArtifactId());
					analyzer.setProperty("mvnVersion", artifact.getBaseVersion());
					analyzer.setProperty("mvnClassifier", artifact.getClassifier());
					String versionString = createOSGiVersion(artifact).toString();
					analyzer.setProperty("generatedOSGiVersion", versionString);
					for (String property : instructions.stringPropertyNames()) {
						// See https://github.com/bndtools/bnd/issues/5659
						String trimValue = instructions.getProperty(property).trim();
						analyzer.setProperty(property, trimValue);
					}
					for (WrappedBundle dep : depends) {
						analyzer.addClasspath(dep.getJar());
						analyzer.removeClose(dep.getJar());
					}
					analyzerJar.setManifest(analyzer.calcManifest());
					analyzerJar.write(wrapArtifactFile);
					for (String err : analyzer.getErrors()) {
						if (err.contains("Classes found in the wrong directory")) {
							// ignore message from BND not supporting MR jars...
							continue;
						}
						messages.add(new ProcessingMessage(artifact, Type.ERROR, err));
					}
					for (String warn : analyzer.getWarnings()) {
						messages.add(new ProcessingMessage(artifact, Type.WARN, warn));
					}
				}
				wrapArtifactFile.setLastModified(originalFile.lastModified());
				visited.put(node, wrappedNode = new WrappedBundle(node, depends, key, wrapArtifactFile.toPath(),
						new Jar(wrapArtifactFile), messages));
			} else {
				visited.put(node, wrappedNode = new WrappedBundle(node, depends, key, wrapArtifactFile.toPath(),
						new Jar(wrapArtifactFile), List.of()));
			}
			return wrappedNode;
		}
	}

	private static Jar getCachedJar(Path cacheFile, Path sourceFile) {
		try {
			if (!isOutdated(cacheFile, sourceFile)) {
				return new Jar(cacheFile.toFile());
			}
		} catch (IOException e) {
			// if any I/O error occurs we assume we need to regenerate the data...
			Platform.getLog(MavenBundleWrapper.class)
					.error("Reading cached data for " + cacheFile + " failed, will regenerate the data ...", e);
		}
		return null;
	}

	private static String getInstructionsKey(Properties properties, List<WrappedBundle> depends) {
		Stream<String> instructionsStream = properties == null ? Stream.empty()
				: properties.stringPropertyNames().stream().sorted(String.CASE_INSENSITIVE_ORDER)
						.map(key -> key.toLowerCase() + ":" + properties.getProperty(key));
		Stream<String> dependsStream = depends.stream().map(WrappedBundle::getInstructionsKey).filter(Objects::nonNull)
				.sorted(String.CASE_INSENSITIVE_ORDER).distinct();
		String string = Stream.concat(instructionsStream, dependsStream).collect(Collectors.joining("#"));
		return DigestUtils.md5Hex(string);
	}

	public static Version createOSGiVersion(Artifact artifact) {
		String version = artifact.getVersion();
		return createOSGiVersion(version);
	}

	public static Version createOSGiVersion(Model model) {
		return createOSGiVersion(model.getVersion());
	}

	private static final Pattern DASH = Pattern.compile("-");

	public static Version createOSGiVersion(String version) {
		if (version == null || version.isEmpty()) {
			return new Version(0, 0, 1);
		}
		try {
			version = DASH.matcher(version).replaceFirst(".");
			return Version.parseVersion(version);
		} catch (IllegalArgumentException e) {
			return new Version(0, 0, 1, version);
		}
	}

	public static boolean isOutdated(Path cacheFile, Path sourceFile) throws IOException {
		if (Files.exists(cacheFile)) {
			FileTime sourceTimeStamp = Files.getLastModifiedTime(sourceFile);
			FileTime cacheTimeStamp = Files.getLastModifiedTime(cacheFile);
			return sourceTimeStamp.compareTo(cacheTimeStamp) > 0;
		}
		return true;
	}
}
