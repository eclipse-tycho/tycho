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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.jupiter.api.Test;

public class ReferenceBetweenProductsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProductCanReferenceProductFromDifferentModule() throws Exception {
		Verifier verifier = getVerifier("product.crossReference");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File repositoryProject = new File(verifier.getBasedir(), "eclipse-repository");
		P2RepositoryTool repository = P2RepositoryTool.forEclipseRepositoryModule(repositoryProject);

		// verify that product IUs were create by full publisher and not the
		// dependency-only publisher
		P2RepositoryTool.IU referencingProduct = repository.getUniqueIU("product.crossreference.extending-product");
		assertFalse(referencingProduct.getVersion().contains("qualifier"), referencingProduct.getVersion());
		assertTrue(referencingProduct.getProperties().contains("org.eclipse.equinox.p2.type.product=true"),
				referencingProduct.getProperties().toString());

		P2RepositoryTool.IU referencedProduct = repository.getUniqueIU("product.crossreference.product");
		assertFalse(referencedProduct.getVersion().contains("qualifier"), referencedProduct.getVersion());
		assertTrue(referencedProduct.getProperties().contains("org.eclipse.equinox.p2.type.product=true"),
				referencedProduct.getProperties().toString());
	}
}
