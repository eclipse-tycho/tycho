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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;
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

	@Requirement
	private LegacySupport legacySupport;

	@Requirement
	private Logger log;

	@Override
	public Optional<Dependency> resolve(Path path) {
		MavenSession session = legacySupport.getSession();
		if (session != null && session.isOffline()) {
			return Optional.empty();
		}
		try {
			if (Files.isRegularFile(path)) {
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
				JSONObject node = request.asJson().getBody().getObject();
				if (node.has("response")) {
					JSONObject response = node.getJSONObject("response");
					if (response.has("numFound") && response.getInt("numFound") == 1) {
						JSONObject coordinates = response.getJSONArray("docs").getJSONObject(0);
						Dependency dependency = new Dependency();
						dependency.setArtifactId(coordinates.getString("a"));
						dependency.setGroupId(coordinates.getString("g"));
						dependency.setVersion(coordinates.getString("v"));
						dependency.setType(coordinates.getString("p"));
						return Optional.of(dependency);
					}
				}
			}
		} catch (Exception e) {
			log.debug("Can't check " + path + " from central because of " + e, e);
		}
		return Optional.empty();
	}

	private static String toHexString(byte[] bytes) {
		return IntStream.range(0, bytes.length).mapToObj(i -> String.format("%02X", bytes[i]))
				.collect(Collectors.joining());
	}

}
