/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationFactory;

@Component(role = PdeApplicationManager.class)
public class PdeApplicationManager {

	static MavenRepositoryLocation getRepository(Repository location) {
		if (location == null) {
			return new MavenRepositoryLocation(null, URI.create(TychoConstants.ECLIPSE_LATEST));
		}
		return new MavenRepositoryLocation(location.getId(), URI.create(location.getUrl()));
	}

	private final Map<URI, EclipseApplication> buildIndexCache = new ConcurrentHashMap<>();

	@Requirement
	private EclipseApplicationFactory applicationFactory;

	public EclipseApplication getApplication(MavenRepositoryLocation repository) {
		return buildIndexCache.computeIfAbsent(repository.getURL().normalize(), x -> {
			EclipseApplication application = applicationFactory.createEclipseApplication(repository, "PDE Tools");
			application.addBundle("org.eclipse.pde.core");
			application.addBundle("org.eclipse.osgi.compatibility.state");
			return application;
		});

	}
}
