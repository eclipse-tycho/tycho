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
package org.eclipse.tycho.baseline.analyze;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 * Lookup of all classes provided by the java runtime
 */
public class JrtClasses implements Function<String, Optional<ClassMethods>> {

	private Path rootPath;
	private Map<String, Optional<ClassMethods>> cache = new ConcurrentHashMap<>();

	public JrtClasses(String javaHome) {
		try {
			Map<String, String> map;
			if (javaHome != null) {
				map = Map.of("java.home", javaHome);
			} else {
				map = Map.of();
			}
			FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), map);
			rootPath = fs.getPath("/packages");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Optional<ClassMethods> apply(String className) {
		if (rootPath == null) {
			return Optional.empty();
		}
		return cache.computeIfAbsent(className.replace('.', '/'), path -> lookupJreClass(className));
	}

	private Optional<ClassMethods> lookupJreClass(String classPath) {
//	Paths in the "jrt:/" NIO filesystem are of this form:
//
//		  /modules/$MODULE/$PATH
//		  /packages/$PACKAGE/$MODULE
//
//		where $PACKAGE is a package name (e.g., "java.lang"). A path of the
//		second form names a symbolic link which, in turn, points to the
//		directory under /modules that contains a module that defines that
//		package. Example:
//
//		  /packages/java.lang/java.base -> /modules/java.base
//
//		To find java/sql/Array.class without knowing its module you look up
//		/packages/java.sql, which is a directory, and enumerate its entries.
//		In this case there will be just one entry, a symbolic link named
//		"java.sql", which will point to /modules/java.sql, which will contain
//		java/sql/Array.class.
//
		String packageName = DependencyAnalyzer.getPackageName(classPath);
		Path modulesPath = rootPath.resolve(packageName);
		if (Files.isDirectory(modulesPath)) {
			try (Stream<Path> module = Files.list(modulesPath)) {
				Iterator<Path> iterator = module.iterator();
				while (iterator.hasNext()) {
					Path modulePath = iterator.next();
					Path classFile = modulePath.resolve(classPath + ".class");
					if (Files.isRegularFile(classFile)) {
						return Optional.of(new ClassMethods(Files.readAllBytes(classFile), JrtClasses.this));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Optional.empty();
	}

}
