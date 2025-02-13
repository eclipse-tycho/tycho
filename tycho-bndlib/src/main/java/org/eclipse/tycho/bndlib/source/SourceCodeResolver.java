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
package org.eclipse.tycho.bndlib.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class SourceCodeResolver {

	private ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
	private List<Path> sourcePathList;
	private final Map<Path, SourceFile> sourceFileMap = new ConcurrentHashMap<>();

	public SourceCodeResolver(List<Path> sourcePathList) {
		this.sourcePathList = sourcePathList;
	}

	public SourceFile getCompilationUnit(Path file) {
		return sourceFileMap.computeIfAbsent(file, path -> {
			String source;
			try {
				source = Files.readString(path);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read file " + path, e);
			}
			parser.setSource(source.toCharArray());
			ASTNode ast = parser.createAST(null);
			if (ast instanceof CompilationUnit cu) {
				return new SourceFile(cu, this);
			}
			throw new RuntimeException("Not a java file: " + path);
		});
	}

	public SourceFile getCompilationUnit(String typeName) {
		String fileName = typeName.replace('.', '/') + SourceFile.JAVA_EXTENSION;
		for (Path source : sourcePathList) {
			Path candidate = source.resolve(fileName);
			if (Files.isRegularFile(candidate)) {
				return getCompilationUnit(candidate);
			}
		}
		return null;
	}

}
