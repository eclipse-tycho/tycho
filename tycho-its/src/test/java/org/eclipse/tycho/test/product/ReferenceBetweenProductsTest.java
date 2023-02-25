/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ReferenceBetweenProductsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProductCanReferenceProductFromDifferentModule() throws Exception {
		Verifier verifier = getVerifier("product.crossReference", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File repositoryProject = new File(verifier.getBasedir(), "eclipse-repository");
		P2RepositoryTool repository = P2RepositoryTool.forEclipseRepositoryModule(repositoryProject);

		// verify that product IUs were create by full publisher and not the
		// dependency-only publisher
		P2RepositoryTool.IU referencingProduct = repository.getUniqueIU("product.crossreference.extending-product");
		assertThat(referencingProduct.getVersion(), not(containsString("qualifier")));
		assertThat(referencingProduct.getProperties(), hasItem("org.eclipse.equinox.p2.type.product=true"));

		P2RepositoryTool.IU referencedProduct = repository.getUniqueIU("product.crossreference.product");
		assertThat(referencedProduct.getVersion(), not(containsString("qualifier")));
		assertThat(referencedProduct.getProperties(), hasItem("org.eclipse.equinox.p2.type.product=true"));
	}
}
