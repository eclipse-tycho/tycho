/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Assert;
import org.junit.Test;

public class ExplodedTestDependenciesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testLocalMavenRepository() throws Exception {
        // project that marks org.apache.ant as "exploded" (unpacked) for the test runtime -> supported since TYCHO-340
        Verifier v01 = getVerifier("surefire.bundleUnpack", false);
        v01.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_LATEST.toString());
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();
        // TODO this is only an indirect test; it should test that the bundles nested jars are accessible as file URLs
        File antHome = new File(v01.getBasedir(),
                "tycho340.test/target/work/plugins/org.apache.ant_1.10.8.v20200515-1239");
        Assert.assertTrue(antHome.isDirectory());
    }

}
