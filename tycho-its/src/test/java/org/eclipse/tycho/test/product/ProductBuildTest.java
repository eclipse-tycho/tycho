/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class ProductBuildTest extends AbstractTychoIntegrationTest {

	private static final List<String> REQUIRED_PGP_PROPERTIES = List.of(TychoConstants.PROP_PGP_SIGNATURES,
			TychoConstants.PROP_PGP_KEYS);

	@Test
	public void testMavenDepedencyInTarget() throws Exception {
		Verifier verifier = getVerifier("product.mavenLocation", false);
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testPGPSignedInProduct() throws Exception {
		Verifier verifier = getVerifier("product.pgp", false);
		verifier.executeGoals(Arrays.asList("clean", "install"));
		verifier.verifyErrorFreeLog();
		checkPGP(verifier, "target/repository/artifacts.xml");
		checkPGP(verifier, "target/products/pgp/linux/gtk/x86_64/artifacts.xml");

	}

	protected void checkPGP(Verifier verifier, String repositoryArtifacts) throws IOException {
		File artifactXml = new File(verifier.getBasedir(), repositoryArtifacts);
		assertTrue("required artifacts file " + artifactXml.getAbsolutePath() + " not found!", artifactXml.isFile());
		Document artifactsDocument = XMLParser.parse(artifactXml);
		Optional<Element> optional = artifactsDocument.getChild("repository").getChild("artifacts")
				.getChildren("artifact").stream()
				.filter(element -> element.getAttributeValue("id").equals("org.mockito.mockito-core")).findAny();
		assertTrue("artifact org.mockito.mockito-core not found", optional.isPresent());
		Element element = optional.get();
		Map<String, String> properties = element.getChild("properties").getChildren("property").stream()
				.collect(Collectors.toMap(e -> e.getAttributeValue("name"), e -> e.getAttributeValue("value")));
		for (String property : REQUIRED_PGP_PROPERTIES) {
			assertTrue("property " + property + " is missing", properties.containsKey(property));
			assertFalse("property " + property + " is present but empty", properties.get(property).isBlank());

		}
	}

	@Test
	public void testAdditionOfUpdateRepositories() throws Exception {
		Verifier verifier = getVerifier("product.update_repository", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		TargetEnvironment env = TargetEnvironment.getRunningEnvironment();
		Path baseDir = Path.of(verifier.getBasedir());
		Path productPath = baseDir.resolve("target/products/aProduct");
		Path osPlatformPath = productPath.resolve(Path.of(env.getOs(), env.getWs(), env.getArch()));
		if (env.getOs().equals("macosx")) {
			osPlatformPath = osPlatformPath.resolve("Eclipse.app/Contents/Eclipse");
		}
		Path p2EnginePath = osPlatformPath.resolve("p2/org.eclipse.equinox.p2.engine");

		List<UpdateSiteReference> expectedReferences = List.of(
				new UpdateSiteReference("https://foo.bar.org", null, true),
				new UpdateSiteReference("https://foo.bar.org/releases", "Latest release", true),
				new UpdateSiteReference("https://foo.bar.org/snapshots", "Latest snapshot", false));

		assertUpdateRepositoryReferences(expectedReferences,
				p2EnginePath.resolve(".settings/org.eclipse.equinox.p2.artifact.repository.prefs"));
		assertUpdateRepositoryReferences(expectedReferences,
				p2EnginePath.resolve(".settings/org.eclipse.equinox.p2.metadata.repository.prefs"));

		assertUpdateRepositoryReferences(expectedReferences, p2EnginePath.resolve(
				"profileRegistry/DefaultProfile.profile/.data/.settings/org.eclipse.equinox.p2.artifact.repository.prefs"));
		assertUpdateRepositoryReferences(expectedReferences, p2EnginePath.resolve(
				"profileRegistry/DefaultProfile.profile/.data/.settings/org.eclipse.equinox.p2.metadata.repository.prefs"));
	}

	private record UpdateSiteReference(String uri, String name, boolean enabled) {
	}

	private static void assertUpdateRepositoryReferences(List<UpdateSiteReference> expectedReferences, Path path)
			throws IOException {
		List<String> prefLines = Files.readAllLines(path);
		for (UpdateSiteReference reference : expectedReferences) {
			String preferencePrefix = "repositories/" + reference.uri.replace(":", "\\:").replace("/", "_");
			assertThat(prefLines, hasItem(preferencePrefix + "/uri=" + reference.uri.replace(":", "\\:")));
			assertThat(prefLines, hasItem(preferencePrefix + "/enabled=" + reference.enabled));
			if (reference.name != null) {
				assertThat(prefLines, hasItem(preferencePrefix + "/nickname=" + reference.name));
			} else {
				assertFalse(prefLines.stream().anyMatch(l -> l.startsWith(preferencePrefix + "/nickname=")));
			}
		}
	}
}
