/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.baseline;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.osgi.framework.Constants;

public class BaselineMojoTest extends AbstractTychoIntegrationTest {

	// To run the Maven+Tycho build of a test-case stand alone (e.g. for debugging),
	// just execute a maven build, after running the test case, for the pom:
	// target/projects/<thisTestCassSimpleName>/<testCaseMethodName>/api-bundle/pom.xml
	// add specify the following property on the CLI:
	// -Dbaseline-url=<file-url-to-this-project>/target/projects/<thisTestCassSimpleName>/baselineRepo

	private static File baselineRepo = null;

	@Before
	public void buildBaselineRepository() throws Exception {
		if (baselineRepo == null) {
			File repoLocation = buildBaseRepo();
			baselineRepo = new File("target/projects", getClass().getSimpleName() + "/baselineRepo").getAbsoluteFile();
			FileUtils.copyDirectoryStructure(repoLocation, baselineRepo);
		}
	}

	/**
	 * Compares the baseline against itself...
	 *
	 * @throws Throwable
	 */
	@Test
	public void testUnchangedApi() throws Throwable {
		buildBaselineProject(false, r -> {
		});
	}

	/**
	 * Compares the baseline against itself... but modify the line endings!
	 *
	 * @throws Throwable
	 */
	@Test
	public void testChangedLineEndings() throws Throwable {
		buildBaselineProject(false, projectPath -> {
			for (String file : new String[] { "about.html", "MPL-1.1.txt" }) {
				Path about = projectPath.resolve(file);
				String string = Files.readString(about);
				if (string.contains("\r\n")) {
					string = string.replace("\r\n", "\n");
				} else if (string.contains("\r")) {
					string = string.replace("\r", "\n");
				} else if (string.contains("\n")) {
					string = string.replace("\n", "\r");
				}
				Files.writeString(about, string);
			}
		});
	}

	/**
	 * This adds a method to the interface
	 *
	 * @throws Throwable
	 */
	@Test
	public void testAddMethod() throws Throwable {

		// test adding a default method to the "public" interface
		Verifier verifier = buildBaselineProject(true, r -> {
			Path aClass = r.resolve(Path.of("src", "my", "api", "bundle", "MyApiInterface.java"));
			modifyJavaSourceFile(aClass, cu -> {
				TypeDeclaration apiInterface = (TypeDeclaration) cu.types().get(0);
				addMethod(apiInterface, "concat", m -> {
					setSimpleReturnType(m, "String");
					addSimpleTypeParameter(m, "String", "a");
					addSimpleTypeParameter(m, "String", "b");
				});
			});
		});
		verifyBaselineProblem(verifier, "ADDED", "METHOD", "concat(java.lang.String,java.lang.String)", "1.0.0",
				"1.1.0");
	}

	/**
	 * This adds a internal method to the interface
	 *
	 * @throws Throwable
	 */
	@Test
	public void testAddInternalMethod() throws Throwable {
		// test with "internal" package but extensions disabled
		ThrowingConsumer<Path> addMethodInternal = r -> {
			Path aClass = r.resolve(Path.of("src", "my", "api", "bundle", "internal", "AnInternalInterface.java"));
			modifyJavaSourceFile(aClass, cu -> {
				TypeDeclaration internalInterface = (TypeDeclaration) cu.types().get(0);
				addNoArgsVoidMethod(internalInterface, "newMethodButItIsInternal");
			});

			modifyProjectManifest(r, m -> m.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.1"));
			modifyProjectPom(r, p -> p.setVersion("1.0.1"));
		};

		Verifier verifier = buildBaselineProject(true, addMethodInternal);
		verifyBaselineProblem(verifier, "ADDED", "METHOD", "newMethodButItIsInternal()", "1.0.1", "2.0.0");
		// now enable extensions, then this should pass
		verifier = buildBaselineProject(false, addMethodInternal, "-Dtycho.baseline.extensions=true");
	}

	/**
	 * This adds a resource to the bundle
	 *
	 * @throws Throwable
	 */
	@Test
	public void testAddResource() throws Throwable {

		// if version is not bumped this should fail
		Verifier verifier = buildBaselineProject(true, r -> {
			Files.writeString(r.resolve("src/NewFile.txt"), "");
		});
		verifyBaselineProblem(verifier, "ADDED", "RESOURCE", "NewFile.txt", "1.0.0", "1.0.1");

		// but if we bump the version even the smallest amount it must pass
		buildBaselineProject(false, r -> {
			Files.writeString(r.resolve("src/NewFile.txt"), "");
			modifyProjectManifest(r, m -> m.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.1"));
			modifyProjectPom(r, p -> p.setVersion("1.0.1"));
		});
	}

	/**
	 * This adds a resource to the bundle
	 *
	 */
	@Test
	public void testAddHeader() throws Throwable {

		// if version is not bumped this should fail
		Verifier verifier = buildBaselineProject(true, r -> {
			modifyProjectManifest(r, m -> m.getMainAttributes().putValue("NewHeader", "not in the baseline"));
		});
		verifyBaselineProblem(verifier, "ADDED", "HEADER", "NewHeader:not in the baseline", "1.0.0", "1.0.1");
	}

	/// Helper methods for baseline verifications ///

	private void verifyBaselineProblem(Verifier verifier, String delta, String type, String name, String projectVersion,
			String suggestVersion) throws VerificationException {
		verifyTextInLogMatches(verifier,
				Pattern.compile("\\[ERROR\\].*" + delta + ".*" + type + ".*" + Pattern.quote(name)));
		verifier.verifyTextInLog("Baseline problems found! Project version: " + projectVersion
				+ ", baseline version: 1.0.0, suggested version: " + suggestVersion);
	}

	private Verifier buildBaselineProject(boolean compareShouldFail, ThrowingConsumer<Path> projectModifier,
			String... xargs) throws Throwable {

		Verifier verifier = getBaselineProject("api-bundle");
		// The verify copies the entire directory structure from projects/baseline into
		// a sub-folder of the target-folder. Apply modifications to copied api-bundle
		Path projectRoot = Path.of(verifier.getBasedir(), "api-bundle");
		projectModifier.accept(projectRoot);

		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());
		for (String xarg : xargs) {
			verifier.addCliOption(xarg);
		}

		if (compareShouldFail) {
			assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));
			verifier.verifyTextInLog("Baseline problems found!");
		} else {
			verifier.executeGoals(List.of("clean", "verify"));
		}
		return verifier;
	}

	private File buildBaseRepo() throws Exception, VerificationException {
		Verifier verifier = getBaselineProject("base-repo");
		verifier.addCliOption("-Dtycho.baseline.skip=true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File repoBase = new File(verifier.getBasedir(), "base-repo/site/target/repository");
		assertTrue("base repository was not created at " + repoBase.getAbsolutePath(), repoBase.isDirectory());
		assertTrue("content.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "content.jar").isFile());
		assertTrue("artifacts.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "artifacts.jar").isFile());
		return repoBase;
	}

	private Verifier getBaselineProject(String project) throws Exception {
		Verifier verifier = getVerifier("baseline", false, true);
		verifier.addCliOption("-f");
		verifier.addCliOption(project + "/pom.xml");
		return verifier;
	}

	private static void modifyProjectPom(Path projectRoot, Consumer<Model> pomModifier) throws IOException {
		File pomFile = projectRoot.resolve("pom.xml").toFile();
		Model model = new DefaultModelReader().read(pomFile, null);
		pomModifier.accept(model);
		new DefaultModelWriter().write(pomFile, null, model);
	}

	private static void modifyProjectManifest(Path projectRoot, Consumer<Manifest> manifestModifier)
			throws IOException {
		Manifest manifest;
		Path manifestFile = projectRoot.resolve(JarFile.MANIFEST_NAME);
		try (InputStream in = Files.newInputStream(manifestFile)) {
			manifest = new Manifest(in);
		}
		manifestModifier.accept(manifest);
		try (OutputStream out = Files.newOutputStream(manifestFile)) {
			manifest.write(out);
		}
	}

	// --- Java source file rewriting ---

	private void modifyJavaSourceFile(Path file, Consumer<CompilationUnit> modifier)
			throws IOException, MalformedTreeException, BadLocationException {
		String source = Files.readString(file);
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(source.toCharArray());

		ASTNode ast = parser.createAST(null);
		if (ast instanceof CompilationUnit compilationUnit) {
			compilationUnit.recordModifications();
			modifier.accept(compilationUnit);
			IDocument document = new Document(source);
			TextEdit edit = compilationUnit.rewrite(document, null);
			edit.apply(document);
			Files.writeString(file, document.get());
		}
	}

	private void addMethod(AbstractTypeDeclaration type, String name, Consumer<MethodDeclaration> methodSetup) {
		AST ast = type.getAST();
		MethodDeclaration method = ast.newMethodDeclaration();
		method.setName(ast.newSimpleName(name));
		methodSetup.accept(method);
		type.bodyDeclarations().add(method);
	}

	private void addNoArgsVoidMethod(AbstractTypeDeclaration type, String name) {
		addMethod(type, name, m -> {
		});
	}

	private void setSimpleReturnType(MethodDeclaration method, String typeIdentifier) {
		AST ast = method.getAST();
		Type type = ast.newSimpleType(ast.newName(typeIdentifier));
		method.setReturnType2(type);
	}

	private void addSimpleTypeParameter(MethodDeclaration method, String qualifiedTypeName, String name) {
		AST ast = method.getAST();
		SingleVariableDeclaration declaration = ast.newSingleVariableDeclaration();
		declaration.setName(ast.newSimpleName(name));
		declaration.setType(ast.newSimpleType(ast.newName(qualifiedTypeName)));
		method.parameters().add(declaration);
	}

}
