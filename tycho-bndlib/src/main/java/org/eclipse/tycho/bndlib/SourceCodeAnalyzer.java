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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.tycho.bndlib.source.SourceCodeResolver;
import org.eclipse.tycho.bndlib.source.SourceFile;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.FileResource;

public class SourceCodeAnalyzer implements FileVisitor<Path> {

	private static final String PACKAGE_INFO = "package-info";
	private static final String ANNOTATION_VERSION = "org.osgi.annotation.versioning.Version";
	private static final String ANNOTATION_EXPORT = "org.osgi.annotation.bundle.Export";
	private static final String PACKAGE_INFO_JAVA = PACKAGE_INFO + SourceFile.JAVA_EXTENSION;
	private static final String PACKAGE_INFO_CLASS = PACKAGE_INFO + ".class";

	private Set<String> seenPackages = new HashSet<>();
	private Set<Path> analyzedPath = new HashSet<>();
	Map<PackageRef, Clazz> packageInfoMap = new HashMap<>();
	private Analyzer analyzer;
	private SourceCodeResolver typeResolver;

	public SourceCodeAnalyzer(Analyzer analyzer, SourceCodeResolver typeResolver) {
		this.analyzer = analyzer;
		this.typeResolver = typeResolver;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		String fileName = file.getFileName().toString().toLowerCase();
		if (fileName.endsWith(SourceFile.JAVA_EXTENSION)) {
			boolean packageInfo = fileName.equals(PACKAGE_INFO_JAVA);
			if (packageInfo || analyzedPath.add(file.getParent())) {
				analyzeSourceFile(file, packageInfo);
			}
		}
		return FileVisitResult.CONTINUE;
	}

	private void analyzeSourceFile(Path file, boolean packageInfo) throws IOException {
		SourceFile source = typeResolver.getCompilationUnit(file);
		PackageDeclaration packageDecl = source.getPackage();
		if (packageDecl != null) {
			String packageFqdn = packageDecl.getName().getFullyQualifiedName();
			PackageRef packageRef = analyzer.getPackageRef(packageFqdn);
			if (seenPackages.add(packageFqdn)) {
				// make the package available to bnd analyzer
				analyzer.getContained().put(packageRef);
			}
			if (packageInfo) {
				JDTClazz clazz = new JDTClazz(analyzer, packageRef.getBinary() + "/" + PACKAGE_INFO_CLASS,
						new FileResource(file), analyzer.getTypeRef(packageRef.getBinary() + "/" + PACKAGE_INFO));
				for (Object raw : packageDecl.annotations()) {
					if (raw instanceof Annotation annot) {
						Name typeName = annot.getTypeName();
						String annotationFqdn = typeName.getFullyQualifiedName();
						if (source.isType(annotationFqdn, ANNOTATION_EXPORT)) {
							clazz.addAnnotation(analyzer.getTypeRef(ANNOTATION_EXPORT.replace('.', '/')));
							packageInfoMap.put(packageRef, clazz);
						} else if (source.isType(annotationFqdn, ANNOTATION_VERSION)) {
							String version = getVersionFromAnnotation(annot, source);
							if (version != null) {
								// if the package is exported or not, the version info must be propagated
								analyzer.getContained().put(packageRef, Attrs.create("version", version));
							}
						}
					}
				}
			}
		}
	}

	private String getVersionFromAnnotation(Annotation annot, SourceFile source) {
		if (annot instanceof NormalAnnotation normal) {
			for (Object vp : normal.values()) {
				MemberValuePair pair = (MemberValuePair) vp;
				if ("value".equals(pair.getName().getFullyQualifiedName())) {
					return source.resolve(pair.getValue());
				}
			}
		} else if (annot instanceof SingleMemberAnnotation single) {
			return source.resolve(single.getValue());
		}
		return null;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
