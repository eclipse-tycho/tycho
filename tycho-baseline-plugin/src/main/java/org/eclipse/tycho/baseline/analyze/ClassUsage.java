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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Analyze code that is used by the class itself
 */
public class ClassUsage {

	private Set<MethodSignature> usedMethodSignatures = new HashSet<>();
	private Map<MethodSignature, Collection<String>> classRef = new HashMap<>();

	public ClassUsage(byte[] classbytes, JrtClasses jrt) {
		ClassReader reader = new ClassReader(classbytes);
		reader.accept(new ClassVisitor(Opcodes.ASM9) {

			private String className;

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				this.className = name.replace('/', '.');
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
							boolean isInterface) {
						if (jrt.apply(owner).isPresent()) {
							// ignore references to java provided classes
							return;
						}
						MethodSignature sig = new MethodSignature(owner, name, descriptor);
						classRef.computeIfAbsent(sig, nil -> new TreeSet<>()).add(className);
						usedMethodSignatures.add(sig);
					}
				};
			}
		}, ClassReader.SKIP_FRAMES);
	}

	public Stream<MethodSignature> signatures() {
		return usedMethodSignatures.stream();
	}

	public Collection<String> classRef(MethodSignature mthd) {
		return classRef.getOrDefault(mthd, List.of());
	}
}
