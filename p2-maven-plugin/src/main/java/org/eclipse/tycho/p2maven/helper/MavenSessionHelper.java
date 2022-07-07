/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.helper;

import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Component in helping other components to perform actions in parallel but
 * still maintain the state of the session in the {@link LegacySupport}
 *
 */
@Component(role = MavenSessionHelper.class)
public class MavenSessionHelper {

	@Requirement
	private LegacySupport legacySupport;

	@Requirement
	private Logger log;

	public MavenSession getSession() {
		return Objects.requireNonNull(legacySupport.getSession(),
				"Current Thread " + Thread.currentThread().getName() + " has no session!");
	}

	public ThreadSession createThreadSession() {
		return new ThreadSession(legacySupport);

	}

	public static final class ThreadSession {

		private MavenSession session;
		private LegacySupport legacySupport;

		private ThreadSession(LegacySupport legacySupport) {
			this.legacySupport = legacySupport;
			this.session = Objects.requireNonNull(legacySupport.getSession());
		}

		public AutoCloseableSession attatch(MavenProject project) {
			MavenSession currentThreadSession = legacySupport.getSession();
			MavenSession clone;
			if (currentThreadSession == null) {
				clone = session.clone();
			} else {
				clone = currentThreadSession.clone();
			}
			MavenProject currentProject = clone.getCurrentProject();
			clone.setCurrentProject(project);
			legacySupport.setSession(clone);
			return new AutoCloseableSession() {

				@Override
				public void close() {
					legacySupport.setSession(currentThreadSession);
					clone.setCurrentProject(currentProject);
				}

				@Override
				public MavenSession getSession() {
					return clone;
				}
			};
		}

	}
	
	public static interface AutoCloseableSession extends AutoCloseable {
		@Override
		void close();

		MavenSession getSession();
	}
}
