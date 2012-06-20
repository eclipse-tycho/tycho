/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product.crossReference;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ReferenceBetweenProductsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProductCanReferenceProductFromDifferentModule() throws Exception {
        Verifier verifier = getVerifier("product.crossReference", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        File repositoryProject = new File(verifier.getBasedir(), "eclipse-repository");
        P2RepositoryTool repository = P2RepositoryTool.forEclipseRepositoryModule(repositoryProject);

        // verify that product IUs were create by full publisher and not the dependency-only publisher
        P2RepositoryTool.IU referencingProduct = repository.getUniqueIU("product.crossreference.extending-product");
        assertThat(referencingProduct.getVersion(), not(containsString("qualifier")));
        assertThat(referencingProduct.getProperties(), hasItem("lineUp=true"));

        P2RepositoryTool.IU referencedProduct = repository.getUniqueIU("product.crossreference.product");
        assertThat(referencedProduct.getVersion(), not(containsString("qualifier")));
        assertThat(referencedProduct.getProperties(), hasItem("lineUp=true"));
    }
}
