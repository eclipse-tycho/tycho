/*******************************************************************************
 * Copyright (c) 2023, 2024 Christoph Läubrich and others.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.tycho.bndlib.source.SourceCodeResolver;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * Enhances the analyzed classes by information obtained from the source code
 */
public class SourceCodeAnalyzerPlugin implements AnalyzerPlugin {

	private List<Path> sourcePaths;

	private boolean alreadyRun;

	private SourceCodeAnalyzer codeAnalyzer;

	public SourceCodeAnalyzerPlugin() {
		this(null);
	}

	public SourceCodeAnalyzerPlugin(List<Path> sourcePaths) {
		this.sourcePaths = sourcePaths;
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		if (alreadyRun) {
			return false;
		}
		alreadyRun = true;
		List<Path> sourcePathList = getSourcePath(analyzer);
		codeAnalyzer = new SourceCodeAnalyzer(analyzer, new SourceCodeResolver(sourcePathList));
		for (Path sourcePath : sourcePathList) {
			Files.walkFileTree(sourcePath, codeAnalyzer);
		}
		return false;
	}

	private List<Path> getSourcePath(Analyzer analyzer) {
		if (sourcePaths != null) {
			return sourcePaths;
		}
		if (analyzer instanceof Builder builder) {
			return builder.getSourcePath().stream().map(File::toPath).toList();
		}
		return List.of();
	}

	public Clazz getPackageInfoClass(PackageRef packageRef) {
		if (codeAnalyzer == null) {
			return null;
		}
		return codeAnalyzer.packageInfoMap.get(packageRef);
	}

}
