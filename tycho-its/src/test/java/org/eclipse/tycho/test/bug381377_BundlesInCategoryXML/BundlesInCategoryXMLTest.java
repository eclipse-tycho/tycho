/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug381377_BundlesInCategoryXML;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

/**
 * @author Mickael Istria (Red Hat Inc.)
 */
public class BundlesInCategoryXMLTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/381377_bundlesInCategoryXML", false);
        verifier.getCliOptions().add("-Dp2-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());
        org.junit.Assert.assertTrue(new File(basedir, "target/repository/plugins/org.apache.ant_1.7.1.v20090120-1145.jar").isFile());
        org.junit.Assert.assertTrue(new File(basedir, "target/repository/plugins/org.junit_3.8.2.v20080602-1318.jar").isFile());
    }
}
