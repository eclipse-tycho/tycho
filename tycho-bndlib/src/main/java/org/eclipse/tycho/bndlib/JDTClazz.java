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

import java.util.HashSet;
import java.util.Set;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Resource;

/**
 * An extension to the (bndlib) {@link Clazz} that is capable to return
 * discovered information form the java sources.
 */
final class JDTClazz extends Clazz {
	private Set<TypeRef> annotations = new HashSet<>();
	private TypeRef className;

	JDTClazz(Analyzer analyzer, String path, Resource resource, TypeRef className) {
		super(analyzer, path, resource);
		this.className = className;
	}

	@Override
	public TypeRef getClassName() {
		return className;
	}

	@Override
	public Set<TypeRef> annotations() {
		return annotations;
	}

	void addAnnotation(TypeRef typeRef) {
		annotations.add(typeRef);
	}

}