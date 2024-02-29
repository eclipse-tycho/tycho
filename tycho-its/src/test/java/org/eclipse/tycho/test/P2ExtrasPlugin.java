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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class P2ExtrasPlugin extends AbstractTychoIntegrationTest {

	@Test
	public void testBaseline() throws Exception {

		Verifier verifier = getVerifier("p2extra/baseline", false, true);
//		# Build baseline
//		invoker.goals.1 = verify -DforceContextQualifier=original
//		invoker.buildResult.1 = success
		execute(verifier, true, null, "original", List.of());
//
//		# Same artifact, same x.y.z.qualifier => OK
//		invoker.goals.2 = clean verify -DforceContextQualifier=original
//		invoker.buildResult.2 = success
//		invoker.project.2 = bundle
//		invoker.profiles.2 = reuse,compare-version-with-baselines
		execute(verifier, true, "bundle", "original", List.of("reuse", "compare-version-with-baselines"));
//
//		# Same x.y.z, different qualifier => KO
//		invoker.goals.3 = clean verify -DforceContextQualifier=other
//		invoker.buildResult.3 = failure
//		invoker.project.3 = bundle
//		invoker.profiles.3 = compare-version-with-baselines
		execute(verifier, false, "bundle", "other", List.of("compare-version-with-baselines"));
//
//		# Same x.y.z.qualifier, different content => KO
//		invoker.goals.4 = clean verify -DforceContextQualifier=original
//		invoker.buildResult.4 = failure
//		invoker.project.4 = bundle
//		invoker.profiles.4 = compare-version-with-baselines
		execute(verifier, false, "bundle", "original", List.of("compare-version-with-baselines"));
//
//		# higher version => OK
//		invoker.goals.5 = clean verify -DforceContextQualifier=any
//		invoker.buildResult.5 = success
//		invoker.project.5 = bundleHigherVersion
//		invoker.profiles.5 = compare-version-with-baselines
		execute(verifier, true, "bundleHigherVersion", "any", List.of("compare-version-with-baselines"));
//
//		# lower version => KO
//		invoker.goals.6 = clean verify -DforceContextQualifier=any
//		invoker.buildResult.6 = failure
//		invoker.project.6 = bundleLowerVersion
//		invoker.profiles.6 = compare-version-with-baselines
		execute(verifier, false, "bundleLowerVersion", "any", List.of("compare-version-with-baselines"));
//
//		# Same artifact, same x.y.z.qualifier => OK
//		invoker.goals.7 = clean verify -DforceContextQualifier=original
//		invoker.buildResult.7 = success
//		invoker.project.7 = feature
//		invoker.profiles.7 = reuse,compare-version-with-baselines,with-repo
		execute(verifier, true, "feature", "original", List.of("reuse", "compare-version-with-baselines", "with-repo"));
//
//		# Same x.y.z, different qualifier => KO
//		invoker.goals.8 = clean verify -DforceContextQualifier=other
//		invoker.buildResult.8 = failure
//		invoker.project.8 = feature
//		invoker.profiles.8 = compare-version-with-baselines,with-repo
		execute(verifier, false, "feature", "other", List.of("compare-version-with-baselines", "with-repo"));
//
//		# Same x.y.z.qualifier, different content => KO
//		invoker.goals.9 = clean verify -DforceContextQualifier=original
//		invoker.buildResult.9 = failure
//		invoker.project.9 = feature
//		invoker.profiles.9 = compare-version-with-baselines,with-repo
		execute(verifier, false, "feature", "original", List.of("compare-version-with-baselines", "with-repo"));
//
//		# higher version => OK
//		invoker.goals.10 = clean verify -DforceContextQualifier=any
//		invoker.buildResult.10 = success
//		invoker.project.10 = featureHigherVersion
//		invoker.profiles.10 = compare-version-with-baselines,with-repo
		execute(verifier, true, "featureHigherVersion", "any", List.of("compare-version-with-baselines", "with-repo"));
//
//		# lower version => KO
//		invoker.goals.11 = clean verify -DforceContextQualifier=any
//		invoker.buildResult.11 = failure
//		invoker.project.11 = featureLowerVersion
//		invoker.profiles.11 = compare-version-with-baselines,with-repo
		execute(verifier, false, "featureLowerVersion", "any", List.of("compare-version-with-baselines", "with-repo"));
	}

	private void execute(Verifier verifier, boolean success, String project, String qualifier, List<String> profiles)
			throws VerificationException {
		List<String> goals = new ArrayList<>();
		goals.add("clean");
		goals.add("verify");
		goals.add("-DforceContextQualifier=" + qualifier);
		if (project != null) {
			goals.add("-f");
			goals.add(project);
		}
		for (String p : profiles) {
			goals.add("-P" + p);
		}
		File logFile = new File(verifier.getBasedir(), verifier.getLogFileName());
		logFile.delete();
		if (success) {
			verifier.executeGoals(goals);
			verifier.verifyErrorFreeLog();
		} else {
			try {
				verifier.executeGoals(goals);
			} catch (VerificationException e) {
				verifier.verifyTextInLog("Failed to execute goal org.eclipse.tycho.extras:tycho-p2-extras-plugin");
			}
		}
	}

	@Test
	public void testPublishFeaturesAndBundles_noUnpack() throws Exception {
		final String pluginId = "test_plugin";
		final String featureId = "test_feature.feature.jar";

		Verifier verifier = getVerifier("p2extra/publisherNoUnpack", false, true);
		verifier.executeGoals(List.of("clean", "package"));

		Path contentXml = Path.of(verifier.getBasedir()).resolve("target/repository").resolve("content.xml");
		Element pluginUnitInContentXml = extractUnitFromContentXml(contentXml, pluginId);
		assertFalse("test plugin should not be marked as zipped", hasChildWithZippedAttribute(pluginUnitInContentXml));
		Element featureUnitInContentXml = extractUnitFromContentXml(contentXml, featureId);
		assertTrue("test feature should be marked as zipped", hasChildWithZippedAttribute(featureUnitInContentXml));
	}

	private static Element extractUnitFromContentXml(Path contentXml, String unitName) throws IOException {
		XMLParser parser = new XMLParser();
		Document document = parser.parse(new XMLIOSource(contentXml.toFile()));
		Element unitElement = document.getChild("repository/units");
		List<Element> units = unitElement.getChildren("unit");
		Optional<Element> extractedUnit = units.stream()
				.filter(element -> unitName.equals(element.getAttribute("id").getValue())).findFirst();
		assertTrue(String.format("Unit with name '%s' not found in content.xml with units: %s", unitName, units),
				extractedUnit.isPresent());
		return extractedUnit.get();
	}

	private static boolean hasChildWithZippedAttribute(Element element) {
		if ("zipped".equals(element.getAttributeValue("key"))) {
			return true;
		}
		return element.getChildren().stream().anyMatch(P2ExtrasPlugin::hasChildWithZippedAttribute);
	}

}
