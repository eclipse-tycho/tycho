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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.Test;

/**
 * This integration test builds and tests the demo projects we provide in the
 * repository
 */
public class DemoTest extends AbstractTychoIntegrationTest {

	@Test
	public void testAutomaticManifest() throws Exception {
		Verifier verifier = runDemo("pde-automatic-manifest");
		BundleDescription description = BundlesAction.createBundleDescription(Path
				.of(verifier.getBasedir(), "tycho.demo.service.impl/target/tycho.demo.service.impl-1.0.0-SNAPSHOT.jar")
				.toFile());
		assertNotNull("demo bundle was not packed", description);
		@SuppressWarnings("unchecked")
		Dictionary<String, String> manifest = (Dictionary<String, String>) description.getUserObject();
		assertEquals("Service component not found", "OSGI-INF/tycho.demo.service.impl.InverterServiceImpl.xml",
				manifest.get("Service-Component"));
		assertTrue("tycho.demo.service.api package not imported", Arrays.stream(description.getImportPackages())
				.anyMatch(pkg -> "tycho.demo.service.api".equals(pkg.getName())));
	}

	@Test
	public void testTychoJustJDemo() throws Exception {
		assertIncludesJustJ(new File(runDemo("justj", "-f", "product").getBasedir(),
				"product/target/products/product-with-justj-features"));
		assertIncludesJustJ(new File(runDemo("justj", "-f", "automaticInstall").getBasedir(),
				"automaticInstall/target/products/product-with-justj-features"));
	}

	@Test
	public void testSureFireDemo() throws Exception {
		runDemo("testing/surefire/", "-f", "with-maven-layout");
		runDemo("testing/surefire/", "-f", "with-source-folder");
	}

	@Test
	public void testTychoSureFireDemo() throws Exception {
		runDemo("testing/tycho/", "-f", "standalone");
		runDemo("testing/tycho/", "-f", "samemodule");
	}

	@Test
	public void testTychoBndDemo() throws Exception {
		runDemo("testing/bnd/", "-f", "osgi-test");
		// TODO add a TCK test demo, e.g. when h2 complies to the jdbc spec we can use
		// that as it is small and fast
	}

	@Test
	public void testTychoBndWorkspaceDemo() throws Exception {
		runDemo("bnd-workspace");
	}

	@Test
	public void testTychoMultiReleaseDemo() throws Exception {
		runDemo("multi-release-jar");
	}

	@Test
	public void testTychoBndPdeWorkspaceDemo() throws Exception {
		runDemo("bnd-pde-workspace");
	}

	@Test
	public void testTychoPublishP2Demo() throws Exception {
		runDemo("publish-p2");
	}

	@Test
	public void testP2MavenRepositoryDemo() throws Exception {
		runDemo("p2-maven-site", "deploy", "-DaltDeploymentRepository=snapshot-repo::default::file:maven-repository");
	}

	@Test
	public void testOsgiMavenRepositoryDemo() throws Exception {
		runDemo("osgi-repository", "deploy", "-DaltDeploymentRepository=snapshot-repo::default::file:maven-repository");
	}

	protected Verifier runDemo(String test, String... xargs) throws Exception {
		Verifier verifier = super.getVerifier("../../demo/" + test, true, true);
		for (String xarg : xargs) {
			verifier.addCliOption(xarg);
		}
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		return verifier;
	}
}
