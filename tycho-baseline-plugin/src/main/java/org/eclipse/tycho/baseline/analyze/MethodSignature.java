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

/**
 * Represents a method signature consisting of the owning class name, the method
 * name, and the method descriptor.
 *
 * @param className  the fully qualified class name (using '/' separators)
 * @param methodName the method name
 * @param signature  the method descriptor
 */
public record MethodSignature(String className, String methodName, String signature)
		implements Comparable<MethodSignature> {

	/**
	 * @return the package name derived from the class name
	 */
	public String packageName() {
		String cn = className();
		return DependencyAnalyzer.getPackageName(cn);
	}

	/**
	 * @return a unique identifier combining class name, method name, and descriptor
	 */
	public String id() {
		return className() + "#" + methodName() + signature();
	}

	@Override
	public int compareTo(MethodSignature o) {
		return id().compareTo(o.id());
	}

	/**
	 * @return {@code true} if this is a constructor or static initializer
	 */
	public boolean isContructor() {
		return "<clinit>".equals(methodName) || "<init>".equals(methodName);
	}
}