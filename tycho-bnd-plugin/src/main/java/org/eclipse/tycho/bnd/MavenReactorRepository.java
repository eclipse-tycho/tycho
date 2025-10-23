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
 *******************************************************************************/
package org.eclipse.tycho.bnd;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import javax.inject.Named;
import javax.inject.Singleton;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.version.Version;

@Named
@Singleton
public class MavenReactorRepository extends ResourcesRepository implements RepositoryPlugin {

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
			throws Exception {
		return null;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return Collections.emptyList();
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return Collections.emptySortedSet();
	}

	@Override
	public String getName() {
		return "Maven Reactor";
	}

	@Override
	public String getLocation() {
		return "reactor";
	}

	public void addProject(MavenProject project) {
		addArtifact(project.getArtifact());
		project.getAttachedArtifacts().forEach(this::addArtifact);
	}

	private void addArtifact(Artifact artifact) {
		File file = artifact.getFile();
		if (file == null || !file.getName().endsWith(".jar") || !file.isFile()) {
			return;
		}
		try {
			SupportingResource resource = ResourceBuilder.parse(file, file.toURI());
			add(resource);
		} catch (RuntimeException e) {

		}
	}

}
