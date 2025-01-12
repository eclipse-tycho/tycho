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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Analyze a class about methods it possibly provides to callers
 */
public class ClassMethods {

	private List<ClassDef> classDefs = new ArrayList<>();
	private List<ClassMethodSignature> signatures = new ArrayList<>();
	private Function<String, Optional<ClassMethods>> supplier;
	private Set<MethodSignature> collect;

	public ClassMethods(byte[] classbytes, Function<String, Optional<ClassMethods>> supplier) {
		this.supplier = supplier;
		ClassReader reader = new ClassReader(classbytes);
		reader.accept(new ClassVisitor(DependencyAnalyzer.ASM_API) {

			private ClassDef classDef;

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				if ((access & Opcodes.ACC_PRIVATE) != 0) {
					// private methods can not be called
					return;
				}
				classDef = new ClassDef(access, name, signature, superName, interfaces);
				classDefs.add(classDef);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				signatures.add(new ClassMethodSignature(classDef, access, name, descriptor, signature));
				return null;
			}
		}, ClassReader.SKIP_FRAMES);
	}

	Stream<ClassDef> definitions() {
		return classDefs.stream();
	}

	/**
	 * @return a stream of all method signatures this class can provide
	 */
	Stream<MethodSignature> provides() {
		// all methods declared by our class are provided
		Stream<MethodSignature> declared = signatures.stream().map(cms -> {
			return new MethodSignature(cms.clazz().name(), cms.name(), cms.descriptor());
		});
		// and from the super class, transformed with our class as the classname
		Stream<MethodSignature> supermethods = classDefs.stream().flatMap(cd -> {
			return Optional.ofNullable(cd.superName()).flatMap(cn -> findRef(cn)).stream().flatMap(cm -> cm.provides())
					// constructors are not inheritable
					.filter(ms -> !ms.isContructor()).map(ms -> inherit(cd, ms));
		});
		// and possibly from interfaces
		Stream<MethodSignature> interfaces = classDefs.stream().flatMap(cd -> {
			return Optional.ofNullable(cd.interfaces()).stream().flatMap(Arrays::stream)
					.flatMap(cn -> findRef(cn).stream()).flatMap(cm -> cm.provides())
					// constructors are not inheritable
					.filter(ms -> !ms.isContructor()).map(ms -> inherit(cd, ms));
		});
		Stream<MethodSignature> inherited = Stream.concat(supermethods, interfaces);
		return Stream.concat(declared, inherited).distinct();
	}

	private MethodSignature inherit(ClassDef cd, MethodSignature ms) {
		return new MethodSignature(cd.name(), ms.methodName(), ms.signature());
	}

	private Optional<ClassMethods> findRef(String cn) {
		return supplier.apply(cn);
	}

}
