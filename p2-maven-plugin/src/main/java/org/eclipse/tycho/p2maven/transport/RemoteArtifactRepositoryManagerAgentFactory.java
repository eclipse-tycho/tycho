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

import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.version.TychoVersion;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager")
public class RemoteArtifactRepositoryManagerAgentFactory implements IAgentServiceFactory {

	@Requirement
	Logger logger;

	@Requirement
	IRepositoryIdManager repositoryIdManager;

	@Requirement
	MavenAuthenticator authenticator;

	@Requirement
	protected LegacySupport mavenContext;

	@Override
	public Object createService(IProvisioningAgent agent) {
		IArtifactRepositoryManager plainRepoManager = (IArtifactRepositoryManager) new ArtifactRepositoryComponent()
				.createService(agent);
		if (getDisableP2MirrorsConfiguration()) {
			plainRepoManager = new P2MirrorDisablingArtifactRepositoryManager(plainRepoManager, logger);
		}
		return new RemoteArtifactRepositoryManager(plainRepoManager, repositoryIdManager, authenticator);
	}

	private boolean getDisableP2MirrorsConfiguration() {
		String deprecatedKey = "tycho.disableP2Mirrors";
		String deprecatedValue = getMirrorProperty(deprecatedKey);

		if (deprecatedValue != null) {
			logger.info("Using " + deprecatedKey
					+ " to disable P2 mirrors is deprecated, use the property eclipse.p2.mirrors instead, see https://tycho.eclipseprojects.io/doc/"
					+ TychoVersion.getTychoVersion() + "/SystemProperties.html for details.");
			return Boolean.parseBoolean(deprecatedValue);
		}

		String key = "eclipse.p2.mirrors";
		String value = getMirrorProperty(key);

		if (value != null) {
			// eclipse.p2.mirrors false -> disable mirrors
			return !Boolean.parseBoolean(value);
		}
		return false;

	}

	private String getMirrorProperty(String key) {
		String value = System.getProperty(key);
		if (key == null && mavenContext.getSession() != null) {
			key = mavenContext.getSession().getSystemProperties().getProperty(key);

			if (key == null) {
				key = mavenContext.getSession().getUserProperties().getProperty(key);
			}
		}
		return value;
	}
}
