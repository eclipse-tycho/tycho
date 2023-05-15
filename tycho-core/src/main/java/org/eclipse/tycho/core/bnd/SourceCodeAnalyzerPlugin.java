/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.bnd;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * Enhances the analyzed classes by information obtained from the source code
 */
class SourceCodeAnalyzerPlugin implements AnalyzerPlugin {

    private static final String ANNOTATION_VERSION = "org.osgi.annotation.versioning.Version";
    private static final String ANNOTATION_EXPORT = "org.osgi.annotation.bundle.Export";
    private static final String PACKAGE_INFO_JAVA = "package-info.java";
    private List<Path> sourcePaths;

    public SourceCodeAnalyzerPlugin(List<Path> sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        Descriptors descriptors = new Descriptors();
        Set<String> seenPackages = new HashSet<>();
        Set<Path> analyzedPath = new HashSet<>();
        for (Path sourcePath : sourcePaths) {
            Files.walkFileTree(sourcePath, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".java")) {
                        boolean packageInfo = fileName.equals(PACKAGE_INFO_JAVA);
                        if (packageInfo || analyzedPath.add(file.getParent())) {
                            String source = Files.readString(file);
                            parser.setSource(source.toCharArray());
                            ASTNode ast = parser.createAST(null);
                            if (ast instanceof CompilationUnit cu) {
                                PackageDeclaration packageDecl = cu.getPackage();
                                if (packageDecl != null) {
                                    String packageFqdn = packageDecl.getName().getFullyQualifiedName();
                                    if (seenPackages.add(packageFqdn)) {
                                        //make the package available to bnd analyzer
                                        PackageRef packageRef = descriptors.getPackageRef(packageFqdn);
                                        analyzer.getContained().put(packageRef);
                                    }
                                    if (packageInfo) {
                                        //check for export annotations
                                        boolean export = false;
                                        String version = null;
                                        for (Object raw : packageDecl.annotations()) {
                                            if (raw instanceof Annotation annot) {
                                                String annotationFqdn = annot.getTypeName().getFullyQualifiedName();
                                                if (ANNOTATION_EXPORT.equals(annotationFqdn)) {
                                                    export = true;
                                                } else if (ANNOTATION_VERSION.equals(annotationFqdn)) {
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
                                        if (export) {
                                            PackageRef packageRef = descriptors.getPackageRef(packageFqdn);
                                            if (version == null) {
                                                analyzer.getContained().put(packageRef);
                                            } else {
                                                analyzer.getContained().put(packageRef,
                                                        Attrs.create("version", version));
                                            }
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

}
