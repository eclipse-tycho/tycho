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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

@Named
@Singleton
public class MavenAuthenticator extends Authenticator {

	private static final Comparator<URI> LONGEST_PREFIX_MATCH = (loc1, loc2) -> {
		// we wan't the longest prefix match, so first sort all uris by their length ...
		String s1 = loc1.normalize().toASCIIString();
		String s2 = loc2.normalize().toASCIIString();
		return Long.compare(s2.length(), s1.length());
	};

	static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
	static final String AUTHORIZATION_HEADER = "Authorization";

	// For some reason maven creates different instances of the component even if
	// there should only be one...
	private static final ThreadLocal<Stack<URI>> locationStack = ThreadLocal.withInitial(Stack::new);
	private static final Map<URI, List<URI>> repositoryChain = new ConcurrentHashMap<>();

	@Inject
	LegacySupport legacySupport;

	@Inject
	ProxyHelper proxyHelper;

	@Inject
	IRepositoryIdManager repositoryIdManager;

	@Inject
	MavenRepositorySettings mavenRepositorySettings;

	@Inject
	Logger log;

	private List<MavenRepositoryLocation> repositoryLocations;

	public Credentials getServerCredentials(URI requestUri) {
		Stack<URI> stack = locationStack.get();
		Stream<URI> repoStream;
		if (stack.isEmpty()) {
			repoStream = getLongestPrefixStream(requestUri);
		} else {
			List<URI> list = new ArrayList<>(stack);
			Collections.reverse(list);
			repoStream = list.stream();
		}
		List<MavenRepositoryLocation> locations = getMavenLocations();
		return Stream.concat(Stream.of(requestUri),
				repoStream.takeWhile(repo -> Objects.equals(repo.getHost(), requestUri.getHost()))).flatMap(uri -> {
			log.debug("Fetching credentials for " + uri);
			return locations.stream().filter(loc -> uriPrefixMatch(uri, loc.getURL()));
		}).map(mavenRepositorySettings::getCredentials).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private boolean uriPrefixMatch(URI matchUri, URI prefixUri) {
		String prefix = prefixUri.normalize().toASCIIString();
		String match = matchUri.normalize().toASCIIString();
		if (match.startsWith(prefix)) {
			log.debug("Found matching " + prefixUri + " for " + matchUri);
			return true;
		}
		log.debug(prefixUri + " does not match (prefix = " + prefix + ", to match = " + match + ")");
		return false;
	}

	private Stream<URI> getLongestPrefixStream(URI requestUri) {
		
		List<URI> list = repositoryChain.entrySet().stream().filter(entry -> uriPrefixMatch(requestUri, entry.getKey()))
				.sorted(Comparator.comparing(Entry::getKey, LONGEST_PREFIX_MATCH))
				.flatMap(entry -> entry.getValue().stream()).toList();
		
		return list.stream();
	}

	private List<MavenRepositoryLocation> getMavenLocations() {
		Stream<MavenRepositoryLocation> locations = getRepositoryLocations().stream();
		locations = Stream.concat(locations, repositoryIdManager.getKnownMavenRepositoryLocations());
		List<MavenRepositoryLocation> sorted = locations
				.sorted(Comparator.comparing(MavenRepositoryLocation::getURL, LONGEST_PREFIX_MATCH)).toList();
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

	public void enterLoad(URI location) {
		log.debug("Enter loading repository " + location);
		Stack<URI> stack = locationStack.get();
		if (!stack.isEmpty()) {
			List<URI> list = new ArrayList<>(locationStack.get());
			Collections.reverse(list);
			repositoryChain.putIfAbsent(location.normalize(), list);
		}
		stack.push(location);
	}

	public void exitLoad() {
		URI pop = locationStack.get().pop();
		log.debug("Exit loading repository " + pop);
	}

	private synchronized List<MavenRepositoryLocation> getRepositoryLocations() {
		if (repositoryLocations == null) {
			MavenSession session = legacySupport.getSession();
			if (session == null) {
				return List.of();
			} else {
				List<MavenProject> projects = Objects.requireNonNullElse(session.getProjects(),
						Collections.emptyList());
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
		return repositoryLocations;
	}

}
