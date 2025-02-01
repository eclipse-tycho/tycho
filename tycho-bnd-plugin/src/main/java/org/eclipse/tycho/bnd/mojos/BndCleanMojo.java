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
package org.eclipse.tycho.bnd.mojos;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import aQute.bnd.build.Project;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class BndCleanMojo extends AbstractBndProjectMojo {

	/**
	 * The filename of the tycho generated POM file.
	 */
	@Parameter(defaultValue = ".tycho-consumer-pom.xml", property = "tycho.bnd.consumerpom.file")
	protected String tychoPomFilename;

	/**
	 * The directory where the tycho generated POM file will be written to.
	 */
	@Parameter(defaultValue = "${project.basedir}", property = "tycho.bnd.consumerpom.directory")
	protected File outputDirectory;

	@Override
	protected void execute(Project project) throws Exception {
		File consumerPom = new File(outputDirectory, tychoPomFilename);
		consumerPom.delete();
		project.clean();
	}

}
