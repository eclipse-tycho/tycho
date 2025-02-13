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

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;

public class SourceFile {

	public static final String JAVA_EXTENSION = ".java";

	private CompilationUnit compilationUnit;
	private SourceCodeResolver sourceCodeResolver;

	public SourceFile(CompilationUnit compilationUnit, SourceCodeResolver sourceCodeResolver) {
		this.compilationUnit = compilationUnit;
		this.sourceCodeResolver = sourceCodeResolver;
	}

	public PackageDeclaration getPackage() {
		return compilationUnit.getPackage();
	}

	@SuppressWarnings({ "unchecked" })
	public List<ImportDeclaration> getImports() {
		return compilationUnit.imports();
	}

	public boolean isType(String annotationFqdn, String annotationVersion) {
		return isType(annotationFqdn, annotationVersion, getImports());
	}

	public SourceFile findType(String clazz) {
		String typeName = getTypeName(clazz, getPackage().getName().getFullyQualifiedName(), getImports());
		return sourceCodeResolver.getCompilationUnit(typeName);
	}

	public String resolve(Expression expression) {
		if (expression instanceof StringLiteral literal) {
			return literal.getLiteralValue();
		}
		if (expression instanceof InfixExpression infix) {
			Expression leftOperand = infix.getLeftOperand();
			Expression rightOperand = infix.getRightOperand();
			Operator operator = infix.getOperator();
			if (Operator.PLUS.equals(operator)) {
				return resolve(leftOperand) + resolve(rightOperand);
			}
		}
		if (expression instanceof QualifiedName qualified) {
			String clazz = qualified.getQualifier().getFullyQualifiedName();
			String constant = qualified.getName().getFullyQualifiedName();
			SourceFile type = findType(clazz);
			if (type != null) {
				return type.getConstantValue(constant);
			}
		}
		if (expression instanceof SimpleName simple) {
			String name = simple.getFullyQualifiedName();
			for (ImportDeclaration imp : getImports()) {
				if (imp.isStatic()) {
					String fqdn = imp.getName().getFullyQualifiedName();
					String suffix = "." + name;
					if (fqdn.endsWith(suffix)) {
						SourceFile type = findType(fqdn.substring(0, fqdn.length() - suffix.length()));
						if (type != null) {
							return type.getConstantValue(name);
						}
					}
				}
			}
		}
		return "";
	}

	private String getConstantValue(String fieldName) {
		FieldVisitor visitor = new FieldVisitor(fieldName);
		compilationUnit.accept(visitor);
		return resolve(visitor.getInitializerExpression());
	}

	private static String getTypeName(String simpleOrFqdn, String pkg, List<ImportDeclaration> imports) {
		if (simpleOrFqdn.contains(".")) {
			// already fqn
			return simpleOrFqdn;
		}
		for (ImportDeclaration importDecl : imports) {
			String name = importDecl.getName().getFullyQualifiedName();
			if (name.endsWith("." + simpleOrFqdn)) {
				// found with imports
				return name;
			}
		}
		// is in the same package (or the default what we do not support here)
		return pkg + "." + simpleOrFqdn;
	}

	private static boolean isType(String simpleOrFqdn, String type, List<ImportDeclaration> imports) {
		if (type.equals(simpleOrFqdn)) {
			return true;
		}
		if (type.endsWith("." + simpleOrFqdn)) {
			for (ImportDeclaration importDecl : imports) {
				if (type.equals(importDecl.getName().getFullyQualifiedName())) {
					return true;
				}
			}
		}
		return false;
	}

}
