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
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuildMojo;

@Mojo(name = "quickfix", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class QuickFixMojo extends AbstractEclipseBuildMojo<QuickFixResult> {

	@Parameter(defaultValue = "${project.build.directory}/quickfix.md", property = "tycho.quickfix.report")
	private File reportFileName;

	/**
	 * Configures the quickfixes to use, as these can be provided by different
	 * bundles, examples are
	 * <ul>
	 * <li><code>org.eclipse.jdt.ui</code> provides resolutions for for java
	 * problems</li>
	 * <li><code>org.eclipse.pde.api.tools.ui</code> provides API tools
	 * resolutions</li>
	 * <li>...</li>
	 * </ul>
	 */
	@Parameter
	private List<String> quickfixes;

	@Override
	protected void handleResult(QuickFixResult result) throws MojoFailureException {
		if (result.isEmpty()) {
			return;
		}
		MarkdownBuilder builder = new MarkdownBuilder(reportFileName);
		List<String> fixes = result.fixes().toList();
		builder.h3("The following " + (fixes.size() > 0 ? "warnings" : "warning") + " has been resolved:");
		fixes.forEach(fix -> {
			builder.addListItem(fix);
			getLog().info("QuickFix: " + fix);
		});
		builder.newLine();
		builder.newLine();
		builder.write();
	}

	@Override
	protected String[] getRequireBundles() {
		Builder<String> builder = Stream.builder();
		builder.accept("org.eclipse.ui.ide");
		if (quickfixes != null) {
			quickfixes.forEach(builder);
		}
		return builder.build().toArray(String[]::new);
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
