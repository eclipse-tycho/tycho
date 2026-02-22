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

/**
 * Analyzes JAR files for class dependencies, determining which methods a
 * bundle provides and which methods its classes use from other bundles.
 */
public class DependencyAnalyzer {

	static final String CLASS_SUFFIX = ".class";
	static final int ASM_API = Opcodes.ASM9;
	private BiConsumer<String, Throwable> errorConsumer;
	private JrtClasses jrtClassResolver;

	/**
	 * Creates a new dependency analyzer.
	 * @param jrtClassResolver the java runtime class resolver
	 *
	 * @param errorConsumer consumer for error messages and their associated
	 *                      exceptions
	 */
	public DependencyAnalyzer(JrtClasses jrtClassResolver, BiConsumer<String, Throwable> errorConsumer) {
		this.jrtClassResolver = jrtClassResolver;
		this.errorConsumer = errorConsumer;
	}

	/**
	 * Extracts the package name from a fully qualified class name.
	 *
	 * @param className the class name using either '/' or '.' separators
	 * @return the package name, or the class name itself if in the default package
	 */
	public static String getPackageName(String className) {
		className = className.replace('/', '.');
		int idx = className.lastIndexOf('.');
		if (idx > 0) {
			return className.substring(0, idx);
		}
		return className;
	}

	/**
	 * Creates a class resolver that covers all dependency artifacts and the JRT
	 * classes.
	 *
	 * @param artifacts        the dependency artifacts to include
	 * @return a function that resolves class names to their method information
	 * @throws MojoFailureException if analysis fails
	 */
	public Function<String, Optional<ClassMethods>> createDependencyClassResolver(DependencyArtifacts artifacts) {
		ClassCollection allClassMethods = new ClassCollection();
		Function<String, Optional<ClassMethods>> function = allClassMethods.chain(jrtClassResolver);
		List<ArtifactDescriptor> list = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
		for (ArtifactDescriptor descriptor : list) {
			File file = descriptor.fetchArtifact().join();
			try {
				analyzeProvides(file, function, allClassMethods);
			} catch (IOException e) {
				errorConsumer.accept("Skip " + descriptor + " for dependency analysis", e);
			}
		}
		return function;
	}

	/**
	 * Analyzes class usage (method invocations) in the given JAR file.
	 *
	 * @param file the JAR file to analyze
	 * @param jre  the JRT class resolver to filter out runtime references
	 * @return a list of class usages found in the JAR
	 * @throws IOException 
	 * @throws MojoFailureException if analysis fails
	 */
	public static List<ClassUsage> analyzeUsage(File file, JrtClasses jre) throws IOException {
		List<ClassUsage> usages = new ArrayList<>();
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
	}
	
	ClassCollection analyzeProvides(File file, Function<String, Optional<ClassMethods>> classResolver) {
		try {
			return analyzeProvides(file, classResolver, null);
		} catch (IOException e) {
			errorConsumer.accept("Can't read " + file + " into a ClassCollection", e);
			return new ClassCollection();
		}
	}

	/**
	 * Analyzes which methods are provided by the classes in the given JAR file,
	 * optionally notifying a consumer for each analyzed class.
	 *
	 * @param file          the JAR file to analyze
	 * @param classResolver the resolver for referenced classes
	 * @param consumer      optional consumer notified for each analyzed class, may
	 *                      be {@code null}
	 * @return the collection of class method information
	 * @throws IOException on IO problem
	 */
	public ClassCollection analyzeProvides(File file, Function<String, Optional<ClassMethods>> classResolver,
			Consumer<ClassMethods> consumer) throws IOException {
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
						errorConsumer.accept("Can't analyze class '" + name + "' because of error while parsing", e);
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
	}



}
