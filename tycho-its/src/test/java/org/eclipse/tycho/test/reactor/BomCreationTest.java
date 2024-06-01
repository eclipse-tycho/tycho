/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.reactor;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.it.Verifier;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class BomCreationTest extends AbstractTychoIntegrationTest {
	private static Verifier verifier;

	@Before
	public void setUp() throws Exception {
		if (verifier == null) {
			verifier = getVerifier("sbom", false);
			// CycloneDX is logging an excessive amount of data on DEBUG level.
			// Way too much for the verifier to handle properly...
			verifier.getCliOptions().remove("-X");
			verifier.executeGoal("verify");
			verifyErrorFreeLog(verifier);
		}
	}

	@Test
	public void verifyBundle() throws Exception {
		String bomPath = getBomPath("example.plugin");
		verifier.verifyFilePresent(bomPath);

		Bom bom = getBom(bomPath);
		List<Dependency> dependencies = bom.getDependencies();
		verifyBundleDependencies(dependencies);
		verifyCommonDependencies(dependencies);
		assertEquals(dependencies.size(), 7);
	}

	@Test
	public void verifyFeature() throws Exception {
		String bomPath = getBomPath("example.feature");
		verifier.verifyFilePresent(bomPath);

		Bom bom = getBom(bomPath);
		List<Dependency> dependencies = bom.getDependencies();
		verifyFeatureDependencies(dependencies);
		verifyBundleDependencies(dependencies);
		verifyCommonDependencies(dependencies);
		assertEquals(dependencies.size(), 8);
	}

	@Test
	public void verifyRepository() throws Exception {
		String bomPath = getBomPath("repository");
		verifier.verifyFilePresent(bomPath);

		Bom bom = getBom(bomPath);
		List<Dependency> dependencies = bom.getDependencies();
		verifyRepositoryDependencies(dependencies);
		verifyBundleDependencies(dependencies);
		verifyCommonDependencies(dependencies);
		// dependency to example.plugin is already satisfied by repository
		// verifyFeatureDependencies(dependencies);
		Dependency feature = getDependency(dependencies,
				"pkg:p2/example.feature@1.0.0.today?classifier=org.eclipse.update.feature&location=https://www.example.p2.repo/");
		assertNull(feature.getDependencies());
		assertEquals(dependencies.size(), 9);
	}

	@Test
	public void verifyProduct() throws Exception {
		String bomPath = getBomPath("product");
		verifier.verifyFilePresent(bomPath);

		Bom bom = getBom(bomPath);
		List<Dependency> dependencies = bom.getDependencies();
		verifyProductDependencies(dependencies);
		// All dependencies are already satisfied by the product
		// verifyBundleDependencies(dependencies);
		// verifyFeatureDependencies(dependencies);
		// verifyRepositoryDependencies(dependencies);
		Dependency dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.beans@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.observable@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.property@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.common@3.18.200.v20231106-1826?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.osgi@3.18.600.v20231110-1900?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.launcher@1.6.600.v20231106-1826?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.launcher.gtk.linux.x86_64@1.2.800.v20231003-1442?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.executable@3.8.2300.v20231106-1826?classifier=org.eclipse.update.feature&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.executable_root.gtk.linux.x86_64@3.8.2300.v20231106-1826?classifier=binary&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:maven/p2.p2.installable.unit/org.eclipse.equinox.executable_root.gtk.linux.x86_64@3.8.2300.v20231106-1826?type=p2-installable-unit");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/example.plugin@1.0.0.today?classifier=osgi.bundle&location=https://www.example.p2.repo/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/example.feature@1.0.0.today?classifier=org.eclipse.update.feature&location=https://www.example.p2.repo/");
		assertNull(dependency.getDependencies());
		//
		assertEquals(dependencies.size(), 14);
	}

	private void verifyBundleDependencies(List<Dependency> dependencies) {
		Dependency dependency = getDependency(dependencies,
				"pkg:p2/example.plugin@1.0.0.today?classifier=osgi.bundle&location=https://www.example.p2.repo/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.core.databinding@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.core.databinding.beans@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.core.databinding.observable@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.core.databinding.property@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertEquals(dependency.getDependencies().size(), 4);
	}

	private void verifyFeatureDependencies(List<Dependency> dependencies) {
		Dependency dependency = getDependency(dependencies,
				"pkg:p2/example.feature@1.0.0.today?classifier=org.eclipse.update.feature&location=https://www.example.p2.repo/");
		verifyDependency(dependency,
				"pkg:p2/example.plugin@1.0.0.today?classifier=osgi.bundle&location=https://www.example.p2.repo/");
		assertEquals(dependency.getDependencies().size(), 1);
	}

	private void verifyRepositoryDependencies(List<Dependency> dependencies) {
		Dependency dependency = getDependency(dependencies,
				"pkg:maven/tycho-demo/repository.eclipse-repository@1.0.0-SNAPSHOT?type=eclipse-repository");
		verifyDependency(dependencies,
				"pkg:p2/example.plugin@1.0.0.today?classifier=osgi.bundle&location=https://www.example.p2.repo/");
		verifyDependency(dependency,
				"pkg:p2/example.feature@1.0.0.today?classifier=org.eclipse.update.feature&location=https://www.example.p2.repo/");
		assertEquals(dependency.getDependencies().size(), 2);
	}

	private void verifyCommonDependencies(List<Dependency> dependencies) {
		Dependency dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.equinox.common@3.18.200.v20231106-1826?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependency,
				"pkg:p2/org.eclipse.osgi@3.18.600.v20231110-1900?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertEquals(dependency.getDependencies().size(), 2);
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.beans@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.observable@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.property@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.common@3.18.200.v20231106-1826?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
		//
		dependency = getDependency(dependencies,
				"pkg:p2/org.eclipse.osgi@3.18.600.v20231110-1900?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertNull(dependency.getDependencies());
	}

	private void verifyProductDependencies(List<Dependency> dependencies) {
		Dependency dependency = getDependency(dependencies,
				"pkg:maven/tycho-demo/example@1.0.0-SNAPSHOT?type=eclipse-repository");
		verifyDependency(dependencies,
				"pkg:p2/example.plugin@1.0.0.today?classifier=osgi.bundle&location=https://www.example.p2.repo/");
		verifyDependency(dependency,
				"pkg:p2/example.feature@1.0.0.today?classifier=org.eclipse.update.feature&location=https://www.example.p2.repo/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.beans@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.observable@1.13.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.core.databinding.property@1.10.100.v20230708-0916?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.equinox.common@3.18.200.v20231106-1826?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		verifyDependency(dependencies,
				"pkg:p2/org.eclipse.osgi@3.18.600.v20231110-1900?classifier=osgi.bundle&location=https://download.eclipse.org/releases/2023-12/");
		assertEquals(dependency.getDependencies().size(), 8);
	}

	private void verifyDependency(Dependency parent, String ref) {
		verifyDependency(parent.getDependencies(), ref);
	}

	private void verifyDependency(List<Dependency> dependencies, String ref) {
		if (Optional.ofNullable(dependencies).stream().flatMap(Collection::stream)
				.noneMatch(dependency -> match(dependency, ref))) {
			fail("No dependency found matching: " + ref);
		}
	}

	private Dependency getDependency(List<Dependency> dependencies, String ref) {
		return dependencies.stream().filter(dependency -> match(dependency, ref)).findFirst().orElseThrow();
	}

	private boolean match(Dependency dependency, String ref) {
		return URLDecoder.decode(dependency.getRef(), StandardCharsets.UTF_8).equals(ref);
	}

	private String getBomPath(String projectName) {
		return projectName + "/target/bom.xml";
	}

	private Bom getBom(String bomPath) throws ParseException {
		Parser parser = new XmlParser();
		File bom = new File(verifier.getBasedir(), bomPath);
		return parser.parse(bom);
	}
}
