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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.it.Verifier;
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
		verifier.setSystemProperty("tycho.localArtifacts", "ignore");
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
			Collection<MavenDependency> roots = maven.getRoots();
			assertEquals(1, roots.size());
			MavenDependency dependency = roots.iterator().next();
			assertEquals("Maven version was not updated correctly in " + targetFile, "1.3.2", dependency.getVersion());
		}
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
