/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.it.Verifier;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenDependency;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.version.TychoVersion;
import org.junit.Test;
import org.w3c.dom.Document;

public class VersionBumpPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testUpdateTarget() throws Exception {
		Verifier verifier = getVerifier("tycho-version-bump-plugin/update-target", false, true);
		String sourceTargetFile = "update-target.target";
		verifier.setSystemProperty("target", sourceTargetFile);
		verifier.executeGoal("org.eclipse.tycho.extras:tycho-version-bump-plugin:" + TychoVersion.getTychoVersion()
				+ ":update-target");
		verifier.verifyErrorFreeLog();
		File targetFile = new File(verifier.getBasedir(), sourceTargetFile);
		try (FileInputStream input = new FileInputStream(targetFile)) {
			Document target = TargetDefinitionFile.parseDocument(input);
			TargetDefinitionFile parsedTarget = TargetDefinitionFile.parse(target, targetFile.getAbsolutePath());
			List<? extends Location> locations = parsedTarget.getLocations();
			InstallableUnitLocation iu = locations.stream().filter(InstallableUnitLocation.class::isInstance)
					.map(InstallableUnitLocation.class::cast).findFirst()
					.orElseThrow(() -> new AssertionError("IU Location not found!"));
			List<? extends Unit> units = iu.getUnits();
			assertEquals(4, units.size());
			assertIUVersion("org.eclipse.equinox.executable.feature.group", "3.8.900.v20200819-0940", units,
					targetFile);
			assertIUVersion("org.eclipse.jdt.feature.group", "3.18.500.v20200902-1800", units, targetFile);
			assertIUVersion("org.eclipse.platform.ide", "4.17.0.I20200902-1800", units, targetFile);
			assertIUVersion("org.eclipse.pde.feature.group", "3.14.500.v20200902-1800", units, targetFile);
			MavenGAVLocation maven = locations.stream().filter(MavenGAVLocation.class::isInstance)
					.map(MavenGAVLocation.class::cast).findFirst()
					.orElseThrow(() -> new AssertionError("Maven Location not found!"));
			MavenDependency dependency = dependencies(maven, "javax.annotation", "javax.annotation-api").findFirst()
					.orElseThrow(() -> new AssertionError("javax.annotation dependency not found"));
			assertEquals("Maven version was not updated correctly in " + targetFile, "1.3.2", dependency.getVersion());
			List<MavenDependency> list = dependencies(maven, "jakarta.annotation", "jakarta.annotation-api").toList();
			assertEquals(2, list.size());
			VersionScheme scheme = new GenericVersionScheme();
			// we can not know the exact latest major version, but we know it must be larger
			// than 3.0
			Version version3 = scheme.parseVersion("3");
			assertTrue("Maven version was not updated correctly in " + targetFile + " for jakarta.annotation-api 1.3.5",
					scheme.parseVersion(list.get(0).getVersion()).compareTo(version3) >= 0);
			assertTrue(
					"No Update for Maven version was expected in " + targetFile + " for jakarta.annotation-api 2.0.0",
					scheme.parseVersion(list.get(1).getVersion()).compareTo(version3) >= 0);
		}
	}

	@Test
	public void testUpdateTargetWithoutMajor() throws Exception {
		Verifier verifier = getVerifier("tycho-version-bump-plugin/update-target", false, true);
		String sourceTargetFile = "update-target.target";
		verifier.setSystemProperty("target", sourceTargetFile);
		verifier.setSystemProperty("allowMajorUpdates", "false");
		verifier.executeGoal("org.eclipse.tycho.extras:tycho-version-bump-plugin:" + TychoVersion.getTychoVersion()
				+ ":update-target");
		verifier.verifyErrorFreeLog();
		File targetFile = new File(verifier.getBasedir(), sourceTargetFile);
		try (FileInputStream input = new FileInputStream(targetFile)) {
			Document target = TargetDefinitionFile.parseDocument(input);
			TargetDefinitionFile parsedTarget = TargetDefinitionFile.parse(target, targetFile.getAbsolutePath());
			List<? extends Location> locations = parsedTarget.getLocations();
			MavenGAVLocation maven = locations.stream().filter(MavenGAVLocation.class::isInstance)
					.map(MavenGAVLocation.class::cast).findFirst()
					.orElseThrow(() -> new AssertionError("Maven Location not found!"));
			List<MavenDependency> list = dependencies(maven, "jakarta.annotation", "jakarta.annotation-api").toList();
			assertEquals(2, list.size());
			assertEquals(
					"No Update for Maven version was expected in " + targetFile + " for jakarta.annotation-api 1.3.5",
					"1.3.5", list.get(0).getVersion());
			assertEquals(
					"Maven version was not updated correctly in " + targetFile + " for jakarta.annotation-api 2.0.0",
					"2.1.1", list.get(1).getVersion());

		}
	}

	private Stream<MavenDependency> dependencies(MavenGAVLocation maven, String g, String a) {
		Collection<MavenDependency> roots = maven.getRoots();
		return roots.stream().filter(md -> md.getGroupId().equals(g)).filter(md -> md.getArtifactId().equals(a));
	}

	private void assertIUVersion(String id, String version, List<? extends Unit> units, File targetFile) {
		for (Unit unit : units) {
			if (unit.getId().equals(id) && unit.getVersion().equals(version)) {
				return;
			}
		}
		fail("Unit with id " + id + " and version " + version + " not found: "
				+ units.stream().map(String::valueOf).collect(Collectors.joining(System.lineSeparator()))
				+ " in target file " + targetFile);

	}

}
