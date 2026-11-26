/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Contributors to the Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.helper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;

/**
 * Gives singletons access to the {@link MavenSession} of the running build.
 * <p>
 * Singletons must not keep the session (or objects taken from it) in a field: in a JVM that runs
 * several builds (e.g. the Maven Daemon) they outlive the session and would keep using the settings
 * of an earlier build. Unlike {@link LegacySupport#getSession()}, this also works on threads that
 * are not managed by Maven (e.g. threads of the HTTP client), because the Maven session scope is
 * not bound to a thread.
 */
@Named
@Singleton
public class MavenSessionHelper {

	@Inject
	private LegacySupport legacySupport;

	@Inject
	private Provider<MavenSession> sessionProvider;

	/**
	 * @return the session of the current thread if there is one, otherwise the session of the
	 *         running build, or <code>null</code> if no build is running
	 */
	public MavenSession getSession() {
		MavenSession session = legacySupport.getSession();
		if (session != null) {
			return session;
		}
		try {
			return sessionProvider.get();
		} catch (RuntimeException e) {
			// not within the session scope (Guice's OutOfScopeException is not visible to plugins)
			return null;
		}
	}
}
