/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product.fileFormat;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class BuildProductWithIgnoredContent extends AbstractTychoIntegrationTest {

    private static final String BUNDLE_IN_PRODUCT_FILE = "org.example.toBeIgnored";
    private static final String FEATURE_IN_PRODUCT_FILE = "org.eclipse.equinox.executable.feature.group";

    @Test
    public void testBundlesInProductAreIgnoredWhenUseFeaturesIsTrue() throws Exception {
        /*
         * Project with a product file which lists a feature, although the useFeatures attribute is
         * false. The current (Indigo) product editor produces such a file when changing the mode in
         * which the content is defined from features to bundles.
         */
        Verifier verifier = getVerifier("product.fileFormat", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        P2RepositoryTool p2Repository = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
        IU product = p2Repository.getUniqueIU("product.uid");

        assertThat(product.getRequiredIds(), not(hasItem(BUNDLE_IN_PRODUCT_FILE)));
        assertThat(product.getRequiredIds(), hasItem(FEATURE_IN_PRODUCT_FILE));
    }
}
