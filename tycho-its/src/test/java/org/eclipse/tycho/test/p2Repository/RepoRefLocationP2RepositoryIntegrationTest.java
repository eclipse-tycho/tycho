/*******************************************************************************
 * Copyright (c) 2019, 2021 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - support repo ref location (453708)
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.equinox.p2.repository.IRepository.ENABLED;
import static org.eclipse.equinox.p2.repository.IRepository.NONE;
import static org.eclipse.equinox.p2.repository.IRepository.TYPE_ARTIFACT;
import static org.eclipse.equinox.p2.repository.IRepository.TYPE_METADATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.RepositoryReference;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class RepoRefLocationP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

	@Test
	public void testRefLocation() throws Exception {

		List<RepositoryReference> allRepositoryReferences = buildAndGetRepositoryReferences(
				"/p2Repository.repositoryRef.location", v -> v.addCliOption("-Dtest-data-repo="
						+ ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString().replace("/", "//")));

		assertEquals(4, allRepositoryReferences.size());
		assertThat(allRepositoryReferences,
				containsInAnyOrder(new RepositoryReference("http://some.where", TYPE_ARTIFACT, NONE),
						new RepositoryReference("http://some.where", TYPE_METADATA, NONE),
						new RepositoryReference("http://some.where.else", TYPE_ARTIFACT, ENABLED),
						new RepositoryReference("http://some.where.else", TYPE_METADATA, ENABLED)));
	}

	@Test
	public void testReferenceFiltering() throws Exception {
		// Of course it is actually a bit pointless to filter explicitly specified
		// references, but it makes the test simple/faster instead of preparing a
		// target-definition with IU-location so that it can be added automatically,
		// which is the main use-case.
		List<RepositoryReference> allRepositoryReferences = buildAndGetRepositoryReferences(
				"/p2Repository.repositoryRef.filter", c -> {
				});
		assertEquals(4, allRepositoryReferences.size());
		assertThat(allRepositoryReferences, containsInAnyOrder( //
				new RepositoryReference("https://download.eclipse.org/tm4e/releases/0.8.1", TYPE_ARTIFACT, ENABLED),
				new RepositoryReference("https://download.eclipse.org/tm4e/releases/0.8.1", TYPE_METADATA, ENABLED),
				new RepositoryReference("https://some.where/from/category", TYPE_ARTIFACT, ENABLED),
				new RepositoryReference("https://some.where/from/category", TYPE_METADATA, ENABLED)));
	}

	@Test
	public void testAdditionOfOnlyProvidingRepos() throws Exception {
		// Of course it is actually a bit pointless to filter explicitly specified
		// references, but it makes the test simple/faster instead of preparing a
		// target-definition with IU-location so that it can be added automatically,
		// which is the main use-case.
		List<RepositoryReference> allRepositoryReferences = buildAndGetRepositoryReferences(
				"/p2Repository.repositoryRef.filter.providing", c -> {
				});

		assertEquals(allRepositoryReferences.stream().map(rr -> rr.uri()).collect(Collectors.joining(", ")), 4,
				allRepositoryReferences.size());
		assertThat(allRepositoryReferences, containsInAnyOrder( //
				new RepositoryReference("https://download.eclipse.org/eclipse/updates/4.29", TYPE_ARTIFACT, ENABLED),
				new RepositoryReference("https://download.eclipse.org/eclipse/updates/4.29", TYPE_METADATA, ENABLED),
				new RepositoryReference("https://download.eclipse.org/cbi/updates/license", TYPE_ARTIFACT, ENABLED),
				new RepositoryReference("https://download.eclipse.org/cbi/updates/license", TYPE_METADATA, ENABLED)));
	}

	@Test
	public void testTargetResolutionWithReferencedRepositoryInclude() throws Exception {
		// <referencedRepositoryMode>include</referencedRepositoryMode> is the default
		Verifier verifier = getVerifier("/p2Repository.repositoryRef.targetresolution.include", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetResolutionWithReferencedRepositoryIgnore() throws Exception {
		Verifier verifier = getVerifier("/p2Repository.repositoryRef.targetresolution.ignore", false);
		try {
			verifier.executeGoal("package");
			fail("Build should fail due to missing transitive dependency dependency");
		} catch (VerificationException e) {
			verifier.verifyTextInLog("requires 'osgi.bundle; org.eclipse.emf.ecore 0.0.0' but it could not be found");
		}
	}

	private List<RepositoryReference> buildAndGetRepositoryReferences(String buildRoot, Consumer<Verifier> setup)
			throws Exception, VerificationException {
		Verifier verifier = getVerifier(buildRoot, false);
		setup.accept(verifier);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		List<RepositoryReference> allRepositoryReferences = p2Repo.getAllRepositoryReferences();
		return allRepositoryReferences;
	}

}
