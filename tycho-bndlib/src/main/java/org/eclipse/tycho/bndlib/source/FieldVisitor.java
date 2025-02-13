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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

class FieldVisitor extends ASTVisitor {

	private String fieldName;
	private Expression initializer;

	FieldVisitor(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		List<?> fragments = node.fragments();
		for (Object fragment : fragments) {
			if (fragment instanceof VariableDeclaration variable) {
				String name = variable.getName().getFullyQualifiedName();
				if (name.equals(fieldName)) {
					initializer = variable.getInitializer();
					return false;
				}
			}
		}
		return true;
	}

	public Expression getInitializerExpression() {
		return initializer;
	}

}
