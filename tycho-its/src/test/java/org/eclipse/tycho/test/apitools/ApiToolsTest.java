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
 *     Hannes Wellman - add verify test case
 *******************************************************************************/
package org.eclipse.tycho.test.apitools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class ApiToolsTest extends AbstractTychoIntegrationTest {
	@Test
	public void testGenerate() throws Exception {
		Verifier verifier = getVerifier("api-tools/api-break", true, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File descriptionFile = new File(verifier.getBasedir(), "bundle1/target/.api_description");
		assertTrue(descriptionFile.getAbsoluteFile() + " not found", descriptionFile.isFile());
		Document document = XMLParser.parse(descriptionFile);
		assertEquals("api-bundle-1_0.0.1-SNAPSHOT", document.getRootElement().getAttribute("name").getValue());
		// TODO enhance project and assert more useful things...
	}

	@Test
	public void testApiBreak() throws Exception {
		Verifier verifier = getVerifier("api-tools/api-break", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")), () -> {
			String msg = "No API errors where detected!";
			try {
				return msg + System.lineSeparator()
						+ verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false).stream()
								.collect(Collectors.joining(System.lineSeparator()));
			} catch (VerificationException e) {
				return msg;
			}
		});
		// check summary output
		verifier.verifyTextInLog("7 API ERRORS");
		verifier.verifyTextInLog("0 API warnings");
		// check error output has source references and lines
		verifier.verifyTextInLog("File ClassA.java at line 5: The method bundle.ClassA.getString() has been removed");
		verifier.verifyTextInLog(
				"File ClassA.java at line 5: The method bundle.ClassA.getCollection() has been removed");
		verifier.verifyTextInLog(
				"File MANIFEST.MF at line 0: The type bundle.InterfaceA has been removed from api-bundle");
		verifier.verifyTextInLog("File ClassA.java at line 7: Missing @since tag on getGreetings()");
		verifier.verifyTextInLog("File ClassA.java at line 11: Missing @since tag on getCollection()");
		verifier.verifyTextInLog("File InterfaceB.java at line 2: Missing @since tag on bundle.InterfaceB");
		verifier.verifyTextInLog(
				"File MANIFEST.MF at line 5: The major version should be incremented in version 0.0.1, since API breakage occurred since version 0.0.1");
		// now check for the build error output
		verifier.verifyTextInLog("on project api-bundle-1: There are API errors:");
		verifier.verifyTextInLog("src/bundle/ClassA.java:5 The method bundle.ClassA.getString() has been removed");
		verifier.verifyTextInLog("src/bundle/ClassA.java:5 The method bundle.ClassA.getCollection() has been removed");
		verifier.verifyTextInLog(
				"META-INF/MANIFEST.MF:0 The type bundle.InterfaceA has been removed from api-bundle-1_0.0.1");
		verifier.verifyTextInLog("src/bundle/ClassA.java:7 Missing @since tag on getGreetings()");
		verifier.verifyTextInLog("src/bundle/ClassA.java:11 Missing @since tag on getCollection()");
		verifier.verifyTextInLog("src/bundle/InterfaceB.java:2 Missing @since tag on bundle.InterfaceB");
		verifier.verifyTextInLog(
				"META-INF/MANIFEST.MF:5 The major version should be incremented in version 0.0.1, since API breakage occurred since version 0.0.1");

		// TODO: check with api-filter
		// TODO: check with second plugin with BREE?
	}

	/**
	 * This test an api compare where there are only embedded jars but nothing in
	 * the output folder, the expectation is that everything works and no API errors
	 * are reported, in case of problems some invalid API error are reported similar
	 * to "The type org.eclipse.pde.build.Constants has been removed from
	 * org.eclipse.pde.build_3.12.200"
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmbeddedJars() throws Exception {
		Verifier verifier = getVerifier("api-tools/embedded-jars", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	/**
	 * This test an api compare where there are missing bin entry for the main
	 * source, the expectation is that everything works and no API errors are
	 * reported, in case of problems some invalid API error are reported similar to
	 * "The type org.eclipse.equinox.p2.ui.RevertProfilePage has been removed from
	 * org.eclipse.equinox.p2.ui"
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoBin() throws Exception {
		Verifier verifier = getVerifier("api-tools/missing-bin", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	/**
	 * This test an api compare where there is a single jar that makes up the
	 * bundle, the expectation is that everything works and no API errors are
	 * reported, in case of problems some invalid API error are reported similar to
	 * "The type org.eclipse.jdt.debug.eval.IEvaluationResult has been removed from
	 * org.eclipse.jdt.debug"
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleJar() throws Exception {
		Verifier verifier = getVerifier("api-tools/single-jar", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testAnnotations() throws Exception {
		Verifier verifier = getVerifier("api-tools/annotations", true, true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testInvalidRepo() throws Exception {
		Verifier verifier = getVerifier("api-tools/single-jar", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools-broken");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")),
				"Did not error on missing repo");
	}

	@Test
	public void testBaselineResolutonFailure_Error() throws Exception {
		Verifier verifier = getVerifier("api-tools/missing-dependency", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools-incomplete");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());
		verifier.addCliOption("-DfailResolutionError=true");

		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")),
				"Did not error on resolution failure");
		verifier.verifyTextInLog("Can't resolve API baseline!");
	}

	@Test
	public void testBaselineResolutonFailure_Warn() throws Exception {
		Verifier verifier = getVerifier("api-tools/missing-dependency", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools-incomplete");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyTextInLog("Can't resolve API baseline");
	}

	@Test
	public void testBaselineResolutonFailure_Default() throws Exception {
		Verifier verifier = getVerifier("api-tools/single-jar", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools-incomplete");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyTextInLog("Can't resolve API baseline");
	}
}
