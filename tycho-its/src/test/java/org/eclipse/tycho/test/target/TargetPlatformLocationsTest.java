/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.target;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TargetPlatformLocationsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testMavenLocation() throws Exception {
		Verifier verifier = getVerifier("target.maven", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		// check that there are no warnings
		assertThrows("Warning about missing digest algorithm was printed to the log", VerificationException.class,
				() -> {
					verifier.verifyTextInLog(
							"No digest algorithm is available to verify download of osgi.bundle,org.apache.velocity");
				});
	}

	@Test
	public void testMavenLocationScopes() throws Exception {
		Verifier verifier = getVerifier("target.maven-scopes", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testMavenArtifactHaveMavenRepoPath() throws Exception {
		Verifier verifier = getVerifier("target.maven", false, true);
		verifier.addCliOption("-DoutputAbsoluteArtifactFilename=true");
		verifier.executeGoal("dependency:list");
		verifier.verifyErrorFreeLog();
		assertFalse("Location for Maven deps should not resolve to cache",
				Files.readString(Path.of(verifier.getBasedir(), verifier.getLogFileName())).contains("p2/osgi"));
	}

	@Test
	public void testMavenArtifactHaveMavenDepsCoordinates() throws Exception {
		Verifier verifier = getVerifier("target.maven", false, true);
		verifier.executeGoal("dependency:list");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("commons-lang:commons-lang:jar:2.4:compile"); // this is a weak assert, should
																				// be
		// improved
	}

	@Test
	public void testMavenLocationMulti() throws Exception {
		Verifier verifier = getVerifier("target.mavenMulti", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	@Ignore(value = "This test is flaky on the buildserver")
	public void testMavenLocationRepositories() throws Exception {
		Verifier verifier = getVerifier("target.mavenRepos", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testDirectoryLocation() throws Exception {
		Verifier verifier = getVerifier("target.directory", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetPlatformArtifactCaching() throws Exception {
		Verifier verifier = getVerifier("target.artifact.caching", false, true);
		verifier.addCliOption("-Dtycho.localArtifacts=default");

		File annotBundleManifestFile = new File(verifier.getBasedir(),
				"target.test/plugins/osgi.annotation.bundle_0.0.1/META-INF/MANIFEST.MF");
		DefaultBundleReader reader = new DefaultBundleReader();
		OsgiManifest annotBundleManifest = reader.loadManifest(annotBundleManifestFile);
		Assert.assertEquals("tycho.test.package", annotBundleManifest.getValue("Export-Package"));
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		List<String> out = Files.lines(annotBundleManifestFile.toPath())
				.filter(line -> !line.contains("Export-Package")).toList();
		Files.write(annotBundleManifestFile.toPath(), out, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);

		assertThrows("Reference to the not exported package did not fail the build", VerificationException.class,
				() -> verifier.executeGoal("verify"));
		verifier.verifyTextInLog(
				" Missing requirement: test.bundle 0.0.1.qualifier requires 'java.package; tycho.test.package 0.0.0' but it could not be found");
	}

	@Test
	public void testMavenLocationAutogeneratedFeature() throws Exception {
		Verifier verifier = getVerifier("target.maven.autofeature", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		Path targetPlatformRepository = Path.of(verifier.getBasedir(),
				"test.product/target/targetPlatformRepository/content.xml");
		try (var stream = Files.newInputStream(targetPlatformRepository)) {
			Xpp3Dom dom = Xpp3DomBuilder.build(stream, StandardCharsets.UTF_8.displayName());
			Xpp3Dom[] children = dom.getChild("units").getChildren("unit");
			Optional<Xpp3Dom> sourceFeature = Stream.of(children)
					.filter(it -> "org.apache.commons.io.feature.source.feature.group".equals(it.getAttribute("id")))
					.findFirst();
			sourceFeature.ifPresentOrElse(it -> {
				Xpp3Dom[] requirements = it.getChild("requires").getChildren("required");
				Assert.assertNotEquals("Expecting requirements for org.apache.commons.io.feature.source.feature.group",
						0, requirements.length);
				Optional<String> badRequirement = Stream.of(requirements)
						.map(requirement -> requirement.getAttribute("name")).filter(name -> name == null
								|| !name.endsWith(".source") && !name.endsWith(".source.feature.jar"))
						.findFirst();
				badRequirement.ifPresent(name -> Assert
						.fail("All requirements are expected to be source requirements, but found '" + name + "'"));
			}, () -> {
				Assert.fail("Expecting to find source feature org.apache.commons.io.feature.source.feature.group");
			});
		} catch (IOException | XmlPullParserException e) {
			Assert.fail("Expecting to find valid XML content at " + targetPlatformRepository);
		}
	}

	@Test
	public void testMavenLocationEclipseFeature() throws Exception {
		Verifier verifier = getVerifier("target.maven.eclipse-feature", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File targetdir = new File(verifier.getBasedir(), "repository/target");
		assertFileExists(targetdir, "repository/features/org.eclipse.jgit_6.1.0.202203080745-r.jar");
	}

	@Test
	public void testMavenLocationTransitiveFeature() throws Exception {
		Verifier verifier = getVerifier("target.maven-deps", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetDefinedInRepositories() throws Exception {
		Verifier verifier = getVerifier("target.userepositories", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetRepositoryLocation() throws Exception {
		Verifier verifier = getVerifier("target.repository", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}
