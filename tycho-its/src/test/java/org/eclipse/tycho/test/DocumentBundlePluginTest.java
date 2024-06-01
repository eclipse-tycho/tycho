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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

public class DocumentBundlePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testGenerateIndex() throws Exception {
		Verifier verifier = getVerifier("document-bundle-plugin/build-help-index", true, true);
		assertThrows(VerificationException.class, () -> verifier.executeGoal("package"));
		// a full test seems rather complex, so what we can do here is call the test and
		// check that the mojo was executed successful (even though it produces an
		// error)
		verifier.verifyTextInLog("Help documentation could not be indexed completely");
	}

	@Test
	public void test1() throws Exception {
		Verifier verifier = getVerifier("document-bundle-plugin/test1", true, true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		/*******************************************************************************
		 * Copyright (c) 2014 IBH SYSTEMS GmbH and others. All rights reserved. This
		 * program and the accompanying materials are made available under the terms of
		 * the Eclipse Public License v1.0 which accompanies this distribution, and is
		 * available at https://www.eclipse.org/legal/epl-v10.html
		 *
		 * Contributors: IBH SYSTEMS GmbH - initial API and implementation
		 *******************************************************************************/

		// check if the encoding is set
		checkFileContains(verifier, "docbundle1/target/javadoc.options.txt", "-encoding UTF-8");

		// check if the doclet artifacts are passed as path
		checkFileContains(verifier, "docbundle1/target/javadoc.options.txt", "-docletpath");

		// check if the transitive dependencies are also resolved
		checkFileContains(verifier, "docbundle1/target/javadoc.options.txt", "tycho-maven-plugin");

		// check if the custom doclet is used
		checkFileContains(verifier, "docbundle1/target/javadoc.options.txt", "-doclet CustomDoclet");

		// check if the expected java doc files are generated
		checkFile(verifier, "docbundle1/target/gen-doc/toc/javadoc.xml", "Missing expected toc file");
		checkFile(verifier, "docbundle1/target/gen-doc/reference/api/element-list", "Missing element list file");
		checkFile(verifier, "docbundle1/target/gen-doc/reference/api/bundle1/SampleClass1.html", "Missing doc file");

		// check if the toc file contains the custom label and custom summary file
		checkFileContains(verifier, "docbundle1/target/gen-doc/toc/javadoc.xml", "reference/api/custom-overview.html");
		checkFileContains(verifier, "docbundle1/target/gen-doc/toc/javadoc.xml", "The custom main label");
	}

	@Test
	public void testAdditionalDeps() throws Exception {
		Verifier verifier = getVerifier("document-bundle-plugin/additionalDepsTest", true, true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		/*******************************************************************************
		 * Copyright (c) 2019 Red Hat Inc. and others. All rights reserved. This program
		 * and the accompanying materials are made available under the terms of the
		 * Eclipse Public License v1.0 which accompanies this distribution, and is
		 * available at https://www.eclipse.org/legal/epl-v10.html
		 *
		 * Contributors: Red Hat Inc. - initial API and implementation
		 *******************************************************************************/

		// check if all the classpath entries are found
		checkFileContains(verifier, "doc.bundle/target/javadoc.options.txt", "/my.bundle/target/classes");
		checkFileContains(verifier, "doc.bundle/target/javadoc.options.txt",
				"/my.bundle/lib/osgi.annotation-7.0.0.jar");

		// check if the expected java doc files are generated
		checkFile(verifier, "doc.bundle/target/tocjavadoc.xml", "Missing expected toc file");
		checkFile(verifier, "doc.bundle/target/reference/api/element-list", "Missing element list file");
		checkFile(verifier, "doc.bundle/target/reference/api/my/bundle/SampleClass1.html", "Missing doc file");

		// check that annotations are represented in the java doc files
		checkFileContains(verifier, "doc.bundle/target/reference/api/my/bundle/package-summary.html",
				"@Version(\"1.0.0\")");
		checkFileContains(verifier, "doc.bundle/target/reference/api/my/bundle/SampleClass1.html", "@ProviderType");

	}

	private void checkFile(Verifier verifier, String fileName, String message) throws Exception {
		File file = new File(verifier.getBasedir(), fileName);
		if (!file.isFile()) {
			throw new Exception(message + ": " + file);
		}
	}

	private void checkFileContains(Verifier verifier, String fileName, String content) throws Exception {
		File file = new File(verifier.getBasedir(), fileName);
		String fileContent = Files.readString(file.toPath());
		if (!fileContent.contains(content)) {
			throw new Exception("Expected content '" + content + "' could not be found in file " + file);
		}
	}

}
