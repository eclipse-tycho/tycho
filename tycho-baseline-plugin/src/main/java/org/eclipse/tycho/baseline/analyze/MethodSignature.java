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

public record MethodSignature(String className, String methodName, String signature)
		implements Comparable<MethodSignature> {

	public String packageName() {
		String cn = className();
		return DependencyAnalyzer.getPackageName(cn);
	}

	public String id() {
		return className() + "#" + methodName() + signature();
	}

	@Override
	public int compareTo(MethodSignature o) {
		return id().compareTo(o.id());
	}

	public boolean isContructor() {
		return "<clinit>".equals(methodName) || "<init>".equals(methodName);
	}
}