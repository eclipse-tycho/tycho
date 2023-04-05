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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.bnd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;

public class MavenProjectJar extends Jar {

	private Path outputFolder;

	public MavenProjectJar(MavenProject project, Predicate<Path> filter) throws IOException {
		super(project.getId());
		outputFolder = Path.of(project.getBuild().getOutputDirectory());
		Files.walkFileTree(outputFolder, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String path = outputFolder.relativize(file).toString();
				putResource(path, new MavenProjectResource(file));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public void setManifest(Manifest manifest) {
		super.setManifest(manifest);
		putResource(JarFile.MANIFEST_NAME, new ManifestResource(manifest));
	}

	@Override
	public boolean putResource(String path, Resource resource, boolean overwrite) {
		if (resource instanceof MavenProjectResource) {
			return super.putResource(path, resource, overwrite);
		}
		Path file = outputFolder.resolve(path);
		try {
			Files.createDirectories(file.getParent());
			if (overwrite || !Files.exists(file)) {
				try (InputStream stream = resource.openInputStream();
						OutputStream outputStream = Files.newOutputStream(file)) {
					stream.transferTo(outputStream);
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return super.putResource(path, new MavenProjectResource(file), overwrite);
	}
}
