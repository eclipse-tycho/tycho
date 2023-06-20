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
package org.eclipse.tycho.p2maven.transport;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

@Component(role = MavenAuthenticator.class)
public class MavenAuthenticator extends Authenticator implements Initializable {

	static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
	static final String AUTHORIZATION_HEADER = "Authorization";
	private ThreadLocal<Stack<URI>> locationStack = ThreadLocal.withInitial(Stack::new);

	@Requirement
	LegacySupport legacySupport;

	@Requirement
	ProxyHelper proxyHelper;

	@Requirement
	IRepositoryIdManager repositoryIdManager;

	@Requirement
	MavenRepositorySettings mavenRepositorySettings;

	@Requirement
	Logger log;

	private List<MavenRepositoryLocation> repositoryLocations;

	public Credentials getServerCredentials(URI requestUri) {
		List<URI> list = new ArrayList<>(locationStack.get());
		Collections.reverse(list);
		Stream<URI> repoStream = list.stream().takeWhile(repo -> repo.getHost().equals(requestUri.getHost()));
		List<MavenRepositoryLocation> locations = getMavenLocations();
		return Stream.concat(Stream.of(requestUri), repoStream).flatMap(uri -> {
			log.info("Fetching credentials for " + uri);
			return locations.stream().filter(loc -> {
				String prefix = loc.getURL().normalize().toASCIIString();
				String match = uri.normalize().toASCIIString();
				if (match.startsWith(prefix)) {
					log.info("Found matching " + loc);
					return true;
				}
				log.info(loc + " does not match (prefix = " + prefix + ", to match = " + match + ")");
				return false;
			});
		}).map(mavenRepositorySettings::getCredentials).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private List<MavenRepositoryLocation> getMavenLocations() {
		Stream<MavenRepositoryLocation> locations = repositoryLocations.stream();
		locations = Stream.concat(locations, repositoryIdManager.getKnownMavenRepositoryLocations());
		List<MavenRepositoryLocation> sorted = locations.sorted((loc1, loc2) -> {
			// we wan't the longest prefix match, so first sort all uris by their length ...
			String s1 = loc1.getURL().normalize().toASCIIString();
			String s2 = loc2.getURL().normalize().toASCIIString();
			return Long.compare(s2.length(), s1.length());
		}).toList();
		return sorted;
	}

	public Authenticator preemtiveAuth(BiConsumer<String, String> headerConsumer, URI uri) {
		// as everything is known and we can't ask the user anyways, preemtive auth is a
		// good choice here to prevent successive requests
		PasswordAuthentication proxyAuth = getAuth(RequestorType.PROXY, uri);
		PasswordAuthentication serverAuth = getAuth(RequestorType.SERVER, uri);
		addAuthHeader(headerConsumer, proxyAuth, PROXY_AUTHORIZATION_HEADER);
		addAuthHeader(headerConsumer, serverAuth, AUTHORIZATION_HEADER);
		return new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				RequestorType type = getRequestorType();
				if (type == RequestorType.PROXY) {
					return proxyAuth;
				}
				if (type == RequestorType.SERVER) {
					return serverAuth;
				}
				return null;
			}
		};
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		try {
			return getAuth(getRequestorType(), getRequestingURL().toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private PasswordAuthentication getAuth(RequestorType type, URI uri) {
		if (type == RequestorType.PROXY) {
			return proxyHelper.getPasswordAuthentication(uri, type);
		} else if (type == RequestorType.SERVER) {
			Credentials credentials = getServerCredentials(uri);
			if (credentials != null) {
				String userName = credentials.getUserName();
				if (userName != null) {
					String password = credentials.getPassword();
					return new PasswordAuthentication(userName,
							password == null ? new char[0] : password.toCharArray());
				}
			}
		}
		return null;
	}

	private void addAuthHeader(BiConsumer<String, String> headerConsumer, PasswordAuthentication authentication,
			String header) {
		if (authentication == null) {
			return;
		}
		String encoding = Base64.getEncoder().encodeToString(
				(authentication.getUserName() + ":" + new String(authentication.getPassword())).getBytes());
		headerConsumer.accept(header, "Basic " + encoding);
	}

	@Override
	public void initialize() throws InitializationException {
		MavenSession session = legacySupport.getSession();
		if (session == null) {
			repositoryLocations = List.of();
		} else {
			List<MavenProject> projects = Objects.requireNonNullElse(session.getProjects(), Collections.emptyList());
			repositoryLocations = projects.stream().map(MavenProject::getRemoteArtifactRepositories)
					.flatMap(Collection::stream).filter(r -> r.getLayout() instanceof P2ArtifactRepositoryLayout)
					.map(r -> {
						try {
							return new MavenRepositoryLocation(r.getId(), new URL(r.getUrl()).toURI());
						} catch (MalformedURLException | URISyntaxException e) {
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
		}

	}

	public void enterLoad(URI location) {
		log.info("Enter loading repository " + location);
		locationStack.get().push(location);
	}

	public void exitLoad() {
		URI pop = locationStack.get().pop();
		log.info("Exit loading repository " + pop);
	}

}
