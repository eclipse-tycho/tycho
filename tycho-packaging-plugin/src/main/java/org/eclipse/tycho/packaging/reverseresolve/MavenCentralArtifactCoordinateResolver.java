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
package org.eclipse.tycho.packaging.reverseresolve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.Field.StringField;
import org.apache.maven.search.api.request.Paging;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.backend.smo.SmoSearchBackend;
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory;
import org.apache.maven.search.backend.smo.SmoSearchResponse;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReproducibleUtils;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

/**
 *
 * Use the maven rest API to find an artifact based on its sha1 sum.
 */
@Singleton
@Named("central")
public class MavenCentralArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	private static final StringField KEY_GROUP_ID = new StringField("g");
	private static final StringField KEY_ARTIFACT_ID = new StringField("a");
	private static final StringField KEY_VERSION = new StringField("v");
	private static final StringField KEY_TYPE = new StringField("p");

	@Inject
	private Logger log;

	@Inject
	MavenContext mavenContext;

	private Map<File, Optional<Dependency>> filesCache = new ConcurrentHashMap<>();

	@Override
	public Optional<Dependency> resolve(Dependency dep, MavenProject project, MavenSession session) {
		if (session.isOffline()) {
			return Optional.empty();
		}
		GAV gav = new GAV(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
		String relativePath = RepositoryLayoutHelper.getRelativePath(gav, dep.getClassifier(), dep.getType(),
				mavenContext);
		File key = getCacheFile(mavenContext.getLocalRepositoryRoot(), relativePath);
		Optional<Dependency> value = filesCache.computeIfAbsent(key, cacheFile -> {
			key.getParentFile().mkdirs();
			if (cacheFile.exists() && !mavenContext.isUpdateSnapshots()) {
				if (cacheFile.length() == 0) {
					return Optional.empty();
				}
				return restoreFromCache(cacheFile);
			}
			return ArtifactCoordinateResolver.getPath(dep).filter(Files::isRegularFile).filter(p -> {
				try {
					return Files.size(p) > 0;
				} catch (IOException e1) {
					return false;
				}
			}).map(path -> {
				try {
					MessageDigest digest = MessageDigest.getInstance("SHA-1");
					byte[] buffer = new byte[8192];
					try (InputStream stream = Files.newInputStream(path)) {
						int read;
						while ((read = stream.read(buffer)) > -1) {
							if (read > 0) {
								digest.update(buffer, 0, read);
							}
						}
					}
					String sha1Hash = toHexString(digest.digest());
					
					SmoSearchBackend searchBackend = SmoSearchBackendFactory.createCsc();
					SmoSearchResponse searchResponse = searchBackend.search(new SearchRequest(new Paging(2), Query.query("1:" + sha1Hash)));
					if (searchResponse.getTotalHits() == 1) {
						Record result = searchResponse.getPage().get(0);
						Dependency dependency = new Dependency();
						dependency.setGroupId(result.getValue(KEY_GROUP_ID));
						dependency.setArtifactId(result.getValue(KEY_ARTIFACT_ID));
						dependency.setVersion(result.getValue(KEY_VERSION));
						dependency.setType(result.getValue(KEY_TYPE));
						cacheResult(cacheFile, dependency);
						return dependency;
					}
					cacheResult(cacheFile, null);
				} catch (Exception e) {
					log.warn("Cannot map " + gav + " @ " + path + ") to Maven Central because of: " + e);
				}
				return null;
			});
		});
		return value.map(Dependency::clone);
	}

	private Optional<Dependency> restoreFromCache(File cacheFile) {
		try {
			Properties properties = new Properties();
			try (FileInputStream stream = new FileInputStream(cacheFile)) {
				properties.load(stream);
			}
			Dependency dependency = new Dependency();
			dependency.setGroupId(properties.getProperty(KEY_GROUP_ID.getFieldName()));
			dependency.setArtifactId(properties.getProperty(KEY_ARTIFACT_ID.getFieldName()));
			dependency.setVersion(properties.getProperty(KEY_VERSION.getFieldName()));
			dependency.setType(properties.getProperty(KEY_TYPE.getFieldName()));
			return Optional.of(dependency);
		} catch (IOException e) {
			// can't read cache file then... try to reload next time.
			cacheFile.delete();
		}
		return Optional.empty();
	}

	private void cacheResult(File cacheFile, Dependency dependency) {
		try {
			if (dependency == null) {
				Files.createFile(cacheFile.toPath());
				return;
			}
			Properties properties = new Properties();
			properties.setProperty(KEY_GROUP_ID.getFieldName(), dependency.getGroupId());
			properties.setProperty(KEY_ARTIFACT_ID.getFieldName(), dependency.getArtifactId());
			properties.setProperty(KEY_VERSION.getFieldName(), dependency.getVersion());
			properties.setProperty(KEY_TYPE.getFieldName(), dependency.getType());
			ReproducibleUtils.storeProperties(properties, cacheFile.toPath());
		} catch (IOException e) {
			// can't create cache file then...
		}
	}

	private static String toHexString(byte[] bytes) {
		return IntStream.range(0, bytes.length).mapToObj(i -> String.format("%02X", bytes[i]))
				.collect(Collectors.joining());
	}

	public static void clearCache(String localRepository, List<Dependency> dependencies) {
		for (Dependency dep : dependencies) {
			GAV gav = new GAV(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
			String relativePath = RepositoryLayoutHelper.getRelativePath(gav, dep.getClassifier(), "jar");
			getCacheFile(new File(localRepository), relativePath).delete();
		}

	}

	private static File getCacheFile(File localRepository, String relativePath) {
		return new File(localRepository, relativePath + ".central.lookup");
	}

}
