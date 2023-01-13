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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

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
	private LegacySupport legacySupport;

	@Requirement
	private Logger log;

	private Map<File, Optional<Dependency>> filesCache = new ConcurrentHashMap<>();

	@Override
	public Optional<Dependency> resolve(Path path) {
		MavenSession session = legacySupport.getSession();
		if (session != null && session.isOffline()) {
			return Optional.empty();
		}
		try {
			if (Files.isRegularFile(path)) {
				File file = path.toFile();
				File key = new File(file.getParentFile(), file.getName() + ".central.lookup");
				Optional<Dependency> find = filesCache.computeIfAbsent(key, cacheFile -> {
					try {
						cacheFile.getParentFile().mkdirs();
						if (cacheFile.exists()) {
							if (cacheFile.length() == 0) {
								return Optional.empty();
							}
							return restoreFromCache(cacheFile);
						}
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
								Optional<Dependency> result = Optional.of(dependency);
								cacheResult(cacheFile, result);
								return result;
							}
						}
						cacheResult(cacheFile, Optional.empty());
					} catch (Exception e) {
						log.debug("Can't check " + path + " from central because of " + e, e);
					}
					return Optional.empty();
				});
				return find.map(Dependency::clone);
			}
		} catch (RuntimeException e) {
		}
		return Optional.empty();
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

	private void cacheResult(File cacheFile, Optional<Dependency> searchResult) {
		try {
			if (searchResult.isEmpty()) {
				Files.createFile(cacheFile.toPath());
				return;
			}
			Dependency dependency = searchResult.get();
			Properties properties = new Properties();
			properties.setProperty(KEY_GROUP_ID, dependency.getGroupId());
			properties.setProperty(KEY_ARTIFACT_ID, dependency.getArtifactId());
			properties.setProperty(KEY_VERSION, dependency.getVersion());
			properties.setProperty(KEY_TYPE, dependency.getType());
			try (FileOutputStream stream = new FileOutputStream(cacheFile)) {
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

}
