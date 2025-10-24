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
package org.eclipse.tycho.p2maven.helper;

import java.net.Authenticator.RequestorType;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

@Named
@Singleton
public class ProxyHelper {

	@Inject
	protected Logger logger;
	@Inject
	protected LegacySupport context;

	@Inject
	protected SettingsDecrypterHelper decrypter;

	private RepositorySystemSession repositorySession;

	@PostConstruct
	public void initialize() {
		MavenSession session = context.getSession();
		if (session != null) {
			repositorySession = session.getRepositorySession();
		}
	}

	public Proxy getProxy(URI uri) {
		if (repositorySession != null) {
			RemoteRepository repository = new RemoteRepository.Builder(null, P2ArtifactRepositoryLayout.ID,
					uri.toASCIIString()).build();
			org.eclipse.aether.repository.Proxy mavenProxy = repositorySession.getProxySelector().getProxy(repository);
			if (mavenProxy != null) {
				return new Proxy(convertType(mavenProxy.getType()),
						new InetSocketAddress(mavenProxy.getHost(), mavenProxy.getPort()));
			}
		}
		return Proxy.NO_PROXY;
	}

	private static Type convertType(String type) {
		if (org.eclipse.aether.repository.Proxy.TYPE_HTTP.equalsIgnoreCase(type)) {
			return Type.HTTP;
		}
		if (org.eclipse.aether.repository.Proxy.TYPE_HTTPS.equalsIgnoreCase(type)) {
			return Type.HTTP;
		}
		return Type.SOCKS;
	}

	public PasswordAuthentication getPasswordAuthentication(URI uri, RequestorType type) {
		if (repositorySession != null) {
			RemoteRepository repository = new RemoteRepository.Builder(null, P2ArtifactRepositoryLayout.ID,
					uri.toASCIIString()).build();
			org.eclipse.aether.repository.Proxy mavenProxy = repositorySession.getProxySelector().getProxy(repository);
			if (mavenProxy != null) {
				Authentication authentication = mavenProxy.getAuthentication();
				if (authentication != null) {
					RemoteRepository repo = new RemoteRepository.Builder(repository).setProxy(mavenProxy).build();
					AuthenticationContext authCtx = AuthenticationContext.forProxy(repositorySession, repo);
					String password = authCtx.get(AuthenticationContext.PASSWORD);
					return new PasswordAuthentication(authCtx.get(AuthenticationContext.USERNAME),
							password == null ? new char[0] : password.toCharArray());
				}
			}
		}
		return null;
	}

}
