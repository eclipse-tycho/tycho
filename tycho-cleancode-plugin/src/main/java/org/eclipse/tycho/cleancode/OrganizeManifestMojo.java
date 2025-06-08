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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.cleancode;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuildMojo;

/**
 * A manifest to perform actions from PDE 'Organize Manifest' (similar to java
 * code cleanups) to cleanup plugins.
 */
@Mojo(name = "manifest", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class OrganizeManifestMojo extends AbstractEclipseBuildMojo<OrganizeManifestResult> {

	@Parameter(defaultValue = "${project.build.directory}/organizeManifest.md", property = "tycho.organizeManifest.report")
	private File reportFileName;

	/**
	 * Calculate 'uses' directive for public packages
	 */
	@Parameter(property = "organizeManifest.calculateUses")
	private boolean calculateUses;

	@Override
	protected void handleResult(OrganizeManifestResult result) throws MojoFailureException {
		MarkdownBuilder builder = new MarkdownBuilder(reportFileName);
		builder.h3("The following Manifest cleanups where applied:");
		if (calculateUses) {
			builder.addListItem("Calculate 'uses' directive for public packages");
		}
		builder.newLine();
		builder.newLine();
		builder.write();
	}

	@Override
	protected OrganizeManifest createExecutable() {
		OrganizeManifest manifest = new OrganizeManifest(project.getBasedir().toPath(), debug);
		if (calculateUses) {
			getLog().info("Organize Manifest: Calculate 'uses' directive for public packages");
		}
		manifest.setCalculateUses(calculateUses);
		return manifest;
	}

	@Override
	protected String getName() {
		return "Organize Manifest";
	}

	@Override
	protected String[] getRequireBundles() {
		return new String[] { "org.eclipse.pde.ui" };
	}

}
