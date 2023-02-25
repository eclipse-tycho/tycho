/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.P2RepositoryTool.withIdAndVersion;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.P2RepositoryTool.IdAndVersion;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.BeforeClass;
import org.junit.Test;

public class QualifierExpansionAndArtifactAssemblyTest extends AbstractTychoIntegrationTest {

	/**
	 * The forcedContextQualifier set in the parent POM.
	 */
	private static final String DEFAULT_QUALIFIER = "20101116-forcedDefault";

	/**
	 * The expanded version of projects with Maven version "1.0.0-SNAPSHOT" which
	 * didn't force a different qualifier than the parent POM.
	 */
	private static final String DEFAULT_VERSION = "1.0.0." + DEFAULT_QUALIFIER;
	private static final String FEATURE_VERSION = "1.2.0.20141230-qualifierOfFeature";
	private static final String BUNDLE_VERSION = "1.2.0.20141230-qualifierOfBundle";

	private static Verifier verifier;
	private static P2RepositoryTool p2Repository;

	@BeforeClass
	public static void executeBuild() throws Exception {
		verifier = new QualifierExpansionAndArtifactAssemblyTest().getVerifier("p2Repository.reactor", false);
		verifier.addCliArgument("-De352-repo=" + P2Repositories.ECLIPSE_352.toString());

		/*
		 * Do not execute "install" to ensure that features and bundles can be included
		 * directly from the build results of the local reactor.
		 */
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		p2Repository = P2RepositoryTool
				.forEclipseRepositoryModule(new File(verifier.getBasedir(), "eclipse-repository"));
	}

	@Test
	public void testCategoryUnitHasInclusionsVersionsExpanded() throws Exception {
		IU categoryIU = p2Repository.getUniqueIU("20141230-qualifierOfRepo" + ".example.category");

		assertThat(categoryIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.category=true"));
		assertThat(categoryIU.getProperties(), hasItem("org.eclipse.equinox.p2.name=Example Category"));

		assertThat(categoryIU.getInclusions(),
				hasItem(withIdAndVersion("prr.example.feature.feature.group", FEATURE_VERSION)));
	}

	@Test
	public void testFeatureUnitHasOwnVersionAndInclusionsExpanded() throws Exception {
		IU featureIU = p2Repository.getIU("prr.example.feature" + ".feature.group", FEATURE_VERSION);

		assertThat(featureIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.group=true"));

		List<IdAndVersion> inclusions = featureIU.getInclusions();
		assertThat(inclusions,
				hasItem(withIdAndVersion("prr.example.included.feature.feature.group", DEFAULT_VERSION)));
		assertThat(inclusions, hasItem(withIdAndVersion("prr.example.bundle", BUNDLE_VERSION)));
		assertThat(inclusions, hasItem(withIdAndVersion("org.eclipse.core.contenttype", "3.4.1.R35x_v20090826-0451"))); // a
																														// bundle
		File featureJar = p2Repository.getFeatureArtifact("prr.example.feature", FEATURE_VERSION);
		assertTrue(featureJar.isFile());
	}

	@Test
	public void testProductUnitHasOwnVersionAndInclusionsExpanded() throws Exception {
		IU featureIU = p2Repository.getIU("prr.example.product", "1.0.0.20141230-qualifierOfRepo");

		assertThat(featureIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.group=true"));
		assertThat(featureIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.product=true"));

		List<IdAndVersion> inclusions = featureIU.getInclusions();
		assertThat(inclusions, hasItem(withIdAndVersion("prr.example.feature.feature.group", FEATURE_VERSION)));
		assertThat(inclusions, hasItem(withIdAndVersion("prr.example.bundle", BUNDLE_VERSION)));
	}

	@Test
	public void testPublishedBundleIU() throws Exception {
		assertThat(p2Repository.getAllUnits(), hasItem(withIdAndVersion("prr.example.bundle", BUNDLE_VERSION)));
		assertTrue(p2Repository.getBundleArtifact("prr.example.bundle", BUNDLE_VERSION).isFile());
	}

	// TODO 373817 test that inclusions in products have the right expanded
	// qualifier

	@Test
	public void testIncludedReactorArtifactsAreAssembled() throws Exception {
		assertThat(p2Repository.getAllUnitIds(), hasItem("prr.example.included.feature" + ".feature.group"));
		assertTrue(p2Repository.getFeatureArtifact("prr.example.included.feature", DEFAULT_VERSION).isFile());

		assertThat(p2Repository.getAllUnitIds(), hasItem("prr.example.included.bundle"));
		assertTrue(p2Repository.getBundleArtifact("prr.example.included.bundle", DEFAULT_VERSION).isFile());
	}

	@Test
	public void testIncludedExternalArtifactIsAssembled() throws Exception {
		assertThat(p2Repository.getAllUnitIds(), hasItem("org.eclipse.core.contenttype"));
		assertTrue(
				p2Repository.getBundleArtifact("org.eclipse.core.contenttype", "3.4.1.R35x_v20090826-0451").isFile());
	}

}
