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

import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.eclipse.tycho.version.TychoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager")
public class RemoteArtifactRepositoryManagerAgentFactory implements IAgentServiceFactory {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final IRepositoryIdManager repositoryIdManager;
	private final MavenAuthenticator authenticator;
	private final MavenPropertyHelper propertyHelper;

	@Inject
	public RemoteArtifactRepositoryManagerAgentFactory(IRepositoryIdManager repositoryIdManager,
													   MavenAuthenticator authenticator,
													   MavenPropertyHelper propertyHelper) {
		this.repositoryIdManager = repositoryIdManager;
		this.authenticator = authenticator;
		this.propertyHelper = propertyHelper;
	}

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
		String deprecatedValue = propertyHelper.getGlobalProperty(deprecatedKey);

		if (deprecatedValue != null) {
			logger.info("Using " + deprecatedKey
					+ " to disable P2 mirrors is deprecated, use the property eclipse.p2.mirrors instead, see https://tycho.eclipseprojects.io/doc/"
					+ TychoVersion.getTychoVersion() + "/SystemProperties.html for details.");
			return getBooleanValue(deprecatedValue);
		}

		String value = propertyHelper.getGlobalProperty("eclipse.p2.mirrors");

		if (value != null) {
			// eclipse.p2.mirrors false -> disable mirrors

			boolean p2MirrorsEnabled = getBooleanValue(value);
			// TODO once we have https://github.com/eclipse-equinox/p2/pull/431 this must be
			// controlled by the agent we create!
			return !p2MirrorsEnabled;
		}
		return false;

	}

	private boolean getBooleanValue(String value) {
		if (value != null && value.isBlank()) {
			return true;
		}
		return Boolean.parseBoolean(value);
	}

}
