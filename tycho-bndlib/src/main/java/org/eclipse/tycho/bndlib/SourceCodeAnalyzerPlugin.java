/*******************************************************************************
 * Copyright (c) 2023, 2024 Christoph Läubrich and others.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * Enhances the analyzed classes by information obtained from the source code
 */
public class SourceCodeAnalyzerPlugin implements AnalyzerPlugin {

	private static final String JAVA_EXTENSION = ".java";
	private static final String PACKAGE_INFO = "package-info";
	private static final String ANNOTATION_VERSION = "org.osgi.annotation.versioning.Version";
	private static final String ANNOTATION_EXPORT = "org.osgi.annotation.bundle.Export";
	private static final String PACKAGE_INFO_JAVA = PACKAGE_INFO + JAVA_EXTENSION;
	private static final String PACKAGE_INFO_CLASS = PACKAGE_INFO + ".class";
	private List<Path> sourcePaths;
	private Map<PackageRef, Clazz> packageInfoMap = new HashMap<>();
	private boolean alreadyRun;

	public SourceCodeAnalyzerPlugin() {
		this(null);
	}

	public SourceCodeAnalyzerPlugin(List<Path> sourcePaths) {
		this.sourcePaths = sourcePaths;
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		if (alreadyRun) {
			return false;
		}
		alreadyRun = true;
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		Set<String> seenPackages = new HashSet<>();
		Set<Path> analyzedPath = new HashSet<>();
		for (Path sourcePath : getSourcePath(analyzer)) {
			Files.walkFileTree(sourcePath, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String fileName = file.getFileName().toString().toLowerCase();
					if (fileName.endsWith(JAVA_EXTENSION)) {
						boolean packageInfo = fileName.equals(PACKAGE_INFO_JAVA);
						if (packageInfo || analyzedPath.add(file.getParent())) {
							String source = Files.readString(file);
							parser.setSource(source.toCharArray());
							ASTNode ast = parser.createAST(null);
							if (ast instanceof CompilationUnit cu) {
								PackageDeclaration packageDecl = cu.getPackage();
								if (packageDecl != null) {
									List<?> imports = cu.imports();
									String packageFqdn = packageDecl.getName().getFullyQualifiedName();
									PackageRef packageRef = analyzer.getPackageRef(packageFqdn);
									if (seenPackages.add(packageFqdn)) {
										// make the package available to bnd analyzer
										analyzer.getContained().put(packageRef);
									}
									if (packageInfo) {
										JDTClazz clazz = new JDTClazz(analyzer,
												packageRef.getBinary() + "/" + PACKAGE_INFO_CLASS,
												new FileResource(file),
												analyzer.getTypeRef(packageRef.getBinary() + "/" + PACKAGE_INFO));
										// check for export annotations
										boolean export = false;
										String version = null;
										for (Object raw : packageDecl.annotations()) {
											if (raw instanceof Annotation annot) {
												Name typeName = annot.getTypeName();
												String annotationFqdn = typeName.getFullyQualifiedName();
												if (isType(annotationFqdn, ANNOTATION_EXPORT, imports)) {
													export = true;
													clazz.addAnnotation(
															analyzer.getTypeRef(ANNOTATION_EXPORT.replace('.', '/')));
												} else if (isType(annotationFqdn, ANNOTATION_VERSION, imports)) {
													if (annot instanceof NormalAnnotation normal) {
														for (Object vp : normal.values()) {
															MemberValuePair pair = (MemberValuePair) vp;
															if ("value"
																	.equals(pair.getName().getFullyQualifiedName())) {
																StringLiteral value = (StringLiteral) pair.getValue();
																version = value.getLiteralValue();
															}
														}
													} else if (annot instanceof SingleMemberAnnotation single) {
														StringLiteral value = (StringLiteral) single.getValue();
														version = value.getLiteralValue();
													}
												}
											}
										}
										if (version != null) {
											// if the package is exported or not, the version info must be propagated
											analyzer.getContained().put(packageRef, Attrs.create("version", version));
										}
										if (export) {
											packageInfoMap.put(packageRef, clazz);
										}
									}
								}
							}
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		}
		return false;
	}

	private List<Path> getSourcePath(Analyzer analyzer) {
		if (sourcePaths != null) {
			return sourcePaths;
		}
		if (analyzer instanceof Builder builder) {
			return builder.getSourcePath().stream().map(File::toPath).toList();
		}
		return List.of();
	}

	public Clazz getPackageInfoClass(PackageRef packageRef) {
		return packageInfoMap.get(packageRef);
	}

	private static boolean isType(String simpleOrFqdn, String type, List<?> imports) {
		if (type.equals(simpleOrFqdn)) {
			return true;
		}
		if (type.endsWith("." + simpleOrFqdn)) {
			for (Object object : imports) {
				if (object instanceof ImportDeclaration importDecl) {
					if (type.equals(importDecl.getName().getFullyQualifiedName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
