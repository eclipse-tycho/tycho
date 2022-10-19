/*******************************************************************************
 * Copyright (c) 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * Similar to the maven-resources-plugin this copies all items from the
 * build.properties bin.includes to the output folder
 */
@Mojo(name = "bin-includes", threadSafe = true)
public class CopyBinIncludesMojo extends AbstractTychoPackagingMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ReactorProject reactorProject = DefaultReactorProject.adapt(project);
		BuildProperties buildProperties = reactorProject.getBuildProperties();
		List<String> binIncludesList = buildProperties.getBinIncludes();
		List<String> binExcludesList = buildProperties.getBinExcludes();
		File basedir = project.getBasedir();
		if (basedir.mkdirs() || basedir.isDirectory()) {
			FileSet fileSet = getFileSet(basedir, binIncludesList, binExcludesList);
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setBasedir(basedir);
			scanner.setExcludes(fileSet.getExcludes());
			scanner.setIncludes(fileSet.getIncludes());
			scanner.setCaseSensitive(fileSet.isCaseSensitive());
			scanner.scan();
			Path basepath = basedir.toPath();
			Path targetPath = Path.of(project.getBuild().getOutputDirectory());
			for (String file : scanner.getIncludedFiles()) {
				try {
					Path source = basepath.resolve(file);
					if (!Files.isRegularFile(source)) {
						getLog().debug("Skip non existing entry " + file);
						continue;
					}
					Path destination = targetPath.resolve(file);
					if (!Files.isDirectory(destination.getParent())) {
						Files.createDirectories(destination.getParent());
					}
					if (Files.isRegularFile(destination)) {
						getLog().debug("Overwrite existing file in target " + destination);
						Files.delete(destination);
					}
					Files.copy(source, destination);
				} catch (IOException e) {
					throw new MojoExecutionException("can't copy " + file, e);
				}
			}
		}
	}

}
