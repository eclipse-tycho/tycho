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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

import kong.unirest.GetRequest;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 *
 * Use the maven rest API to find an artifact based on its sha1 sum.
 */
@Component(role = ArtifactCoordinateResolver.class, hint = "central")
public class MavenCentralArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	private static final int TIMEOUT = Integer.getInteger("tycho.search.central.timeout", 10);

	private static final String KEY_GROUP_ID = "g";
	private static final String KEY_ARTIFACT_ID = "a";
	private static final String KEY_VERSION = "v";
	private static final String KEY_TYPE = "p";

	@Requirement
	private Logger log;

	@Requirement
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
					GetRequest request = Unirest.get("https://search.maven.org/solrsearch/select")
							.queryString("q", "1:" + sha1Hash).queryString("wt", "json");
					request.connectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT));
					request.socketTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT));
					JSONObject node = request.asJson().getBody().getObject();
					if (node.has("response")) {
						JSONObject response = node.getJSONObject("response");
						if (response.has("numFound") && response.getInt("numFound") == 1) {
							JSONObject coordinates = response.getJSONArray("docs").getJSONObject(0);
							Dependency dependency = new Dependency();
							dependency.setGroupId(coordinates.getString(KEY_GROUP_ID));
							dependency.setArtifactId(coordinates.getString(KEY_ARTIFACT_ID));
							dependency.setVersion(coordinates.getString(KEY_VERSION));
							dependency.setType(coordinates.getString(KEY_TYPE));
							cacheResult(cacheFile, dependency);
							return dependency;
						}
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
			dependency.setGroupId(properties.getProperty(KEY_GROUP_ID));
			dependency.setArtifactId(properties.getProperty(KEY_ARTIFACT_ID));
			dependency.setVersion(properties.getProperty(KEY_VERSION));
			dependency.setType(properties.getProperty(KEY_TYPE));
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
			properties.setProperty(KEY_GROUP_ID, dependency.getGroupId());
			properties.setProperty(KEY_ARTIFACT_ID, dependency.getArtifactId());
			properties.setProperty(KEY_VERSION, dependency.getVersion());
			properties.setProperty(KEY_TYPE, dependency.getType());
			try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(cacheFile))) {
				properties.store(stream, null);
			}
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
