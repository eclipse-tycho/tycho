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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuildMojo;

@Mojo(name = "quickfix", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class QuickFixMojo extends AbstractEclipseBuildMojo<QuickFixResult> {

	@Parameter(defaultValue = "${project.build.directory}/quickfix.md", property = "tycho.quickfix.report")
	private File reportFileName;

	@Override
	protected void handleResult(QuickFixResult result) throws MojoFailureException {
		List<String> results = new ArrayList<>();
		if (result.getMarkers() == 0) {
			results.add("Project is clean!");
		} else {
			List<String> fixes = result.fixes().toList();
			if (fixes.isEmpty()) {
				results.add("Nothing has been resolved in this project.");
			} else {
				results.add("The following " + (fixes.size() > 0 ? "warnings" : "warning") + " has been resolved:");
				fixes.forEach(fix -> {
					results.add("- " + fix);
				});
			}
		}
		try {
			Files.writeString(reportFileName.toPath(),
					results.stream().collect(Collectors.joining(System.lineSeparator())));
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
	}

	@Override
	protected String[] getRequireBundles() {
		return new String[] { "org.eclipse.ui.ide", "org.eclipse.jdt.ui" };
	}

	@Override
	protected QuickFix createExecutable() {
		return new QuickFix(project.getBasedir().toPath(), debug);
	}

	@Override
	protected String getName() {
		return "Quick Fix";
	}

}
