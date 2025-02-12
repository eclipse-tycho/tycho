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
package org.eclipse.tycho.bndlib;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;

/**
 * A project builder that injects information from the sources into the builder
 */
public class JdtProjectBuilder extends ProjectBuilder {

	public JdtProjectBuilder(Project project) {
		super(project);
		addBasicPlugin(new SourceCodeAnalyzerPlugin());
	}

	@Override
	public String _packageattribute(String[] args) {
		SourceCodeAnalyzerPlugin analyzerPlugin = getPlugin(SourceCodeAnalyzerPlugin.class);
		try {
			analyzerPlugin.analyzeJar(this);
		} catch (Exception e) {
		}
		return super._packageattribute(args);
	}
}
