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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyArtifacts;
import org.objectweb.asm.Opcodes;

public class DependencyAnalyzer {

	static final String CLASS_SUFFIX = ".class";
	static final int ASM_API = Opcodes.ASM9;
	private BiConsumer<String, Throwable> errorConsumer;

	public DependencyAnalyzer(BiConsumer<String, Throwable> errorConsumer) {
		this.errorConsumer = errorConsumer;
	}

	public static String getPackageName(String className) {
		className = className.replace('/', '.');
		int idx = className.lastIndexOf('.');
		if (idx > 0) {
			return className.substring(0, idx);
		}
		return className;
	}

	public Function<String, Optional<ClassMethods>> createDependencyClassResolver(JrtClasses jrtClassResolver,
			DependencyArtifacts artifacts) throws MojoFailureException {
		ClassCollection allClassMethods = new ClassCollection();
		Function<String, Optional<ClassMethods>> function = allClassMethods.chain(jrtClassResolver);
		List<ArtifactDescriptor> list = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
		for (ArtifactDescriptor descriptor : list) {
			File file = descriptor.fetchArtifact().join();
			analyzeProvides(file, function, allClassMethods);
		}
		return function;
	}

	public static List<ClassUsage> analyzeUsage(File file, JrtClasses jre) throws MojoFailureException {
		List<ClassUsage> usages = new ArrayList<>();
		try {
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					if (name.endsWith(CLASS_SUFFIX)) {
						InputStream stream = jar.getInputStream(jarEntry);
						usages.add(new ClassUsage(stream.readAllBytes(), jre));
					}
				}
			}
			return usages;
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
	}

	public ClassCollection analyzeProvides(File file, Function<String, Optional<ClassMethods>> classResolver)
			throws MojoFailureException {
		return analyzeProvides(file, classResolver, null);
	}

	public ClassCollection analyzeProvides(File file, Function<String, Optional<ClassMethods>> classResolver,
			Consumer<ClassMethods> consumer) throws MojoFailureException {
		try {
			ClassCollection local = new ClassCollection();
			Function<String, Optional<ClassMethods>> resolver = local.chain(classResolver);
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					if (name.endsWith(CLASS_SUFFIX)) {
						InputStream stream = jar.getInputStream(jarEntry);
						ClassMethods methods;
						try {
							methods = new ClassMethods(stream.readAllBytes(), resolver);
						} catch (RuntimeException e) {
							// can't analyze this class, example of errors is
							// java.lang.ArrayIndexOutOfBoundsException: Index 29 out of bounds for length 9
							errorConsumer.accept("Can't analyze class '" + name + "' because of error while parsing",
									e);
							continue;
						}
						if (consumer != null) {
							consumer.accept(methods);
						}
						local.accept(methods);
					}
				}
			}
			return local;
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
	}

}
