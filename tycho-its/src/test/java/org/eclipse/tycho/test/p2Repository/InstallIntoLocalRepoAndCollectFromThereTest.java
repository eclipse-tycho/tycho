/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.P2RepositoryTool.withIdAndVersion;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.BeforeClass;
import org.junit.Test;

public class InstallIntoLocalRepoAndCollectFromThereTest extends AbstractTychoIntegrationTest {

    private static final String BUNDLE_VERSION = "1.2.0.20141230-qualifierOfBundle";

    private static Verifier preparer;
    private static Verifier verifier;
    private static P2RepositoryTool p2Repository;

    @BeforeClass
    public static void executeBuild() throws Exception {
        preparer = new InstallIntoLocalRepoAndCollectFromThereTest().getVerifier("p2Repository.localrepo.source",
                false);
        preparer.getSystemProperties().put("e352-repo", P2Repositories.ECLIPSE_352.toString());
        preparer.executeGoal("install");
        preparer.verifyErrorFreeLog();

        verifier = new InstallIntoLocalRepoAndCollectFromThereTest().getVerifier("p2Repository.localrepo", false,
                false);
        verifier.getSystemProperties().put("e352-repo", P2Repositories.ECLIPSE_352.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        p2Repository = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
    }

    @Test
    public void testPublishedBundleIUFromPreviousReactor() throws Exception {
        verifyNoMirroringErrors("prr.example.bundle");

        assertThat(p2Repository.getAllUnits(), hasItem(withIdAndVersion("prr.example.bundle", BUNDLE_VERSION)));
        assertThat(p2Repository.getBundleArtifact("prr.example.bundle", BUNDLE_VERSION), isFile());

        assertThat(p2Repository.getAllUnits(), hasItem(withIdAndVersion("prr.example.bundle2", BUNDLE_VERSION)));
        assertThat(p2Repository.getBundleArtifact("prr.example.bundle2", BUNDLE_VERSION), isFile());

        assertThat(p2Repository.getAllUnits(), hasItem(withIdAndVersion("prr.example.bundle3", BUNDLE_VERSION)));
        assertThat(p2Repository.getBundleArtifact("prr.example.bundle3", BUNDLE_VERSION), isFile());
    }

    private void verifyNoMirroringErrors(String installableUnitId) throws VerificationException, AssertionError {
        List<?> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
        for (Iterator<?> stream = lines.iterator(); stream.hasNext();) {
            String line = (String) stream.next();
            if (line.startsWith("[WARNING] Mirror tool: Problems resolving provisioning plan.:")
                    && line.contains(installableUnitId)) {
                throw new AssertionError("Unable to mirror included IUs from category.xml into repository: " + line);
            }
        }
    }
}
