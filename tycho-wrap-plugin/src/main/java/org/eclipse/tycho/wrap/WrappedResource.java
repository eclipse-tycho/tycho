/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.wrap;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class WrappedResource implements Resource {

	private Resource resource;
	private Artifact artifact;

	public WrappedResource(Resource resource, Artifact artifact) {
		this.resource = resource;
		this.artifact = artifact;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return resource.getCapabilities(namespace);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return resource.getRequirements(namespace);
	}

	@Override
	public boolean equals(Object obj) {
		return resource.equals(obj);
	}

	@Override
	public int hashCode() {
		return resource.hashCode();
	}

	public Artifact getArtifact() {
		return artifact;
	}

}
