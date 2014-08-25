/*******************************************************************************
 * Copyright (c) 2014 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target.includeSource;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Test;

public class TestIncludeSources extends AbstractTychoIntegrationTest {

    @Test
    public void testWithoutIncludeSourceDefault() throws Exception {
        // A project that depends on bundle.source.
        // will fail unless there are sources
        Verifier verifier = getVerifier("/target.includeSources", false);
        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        try {
            verifier.executeGoals(Arrays.asList("clean", "verify"));
        } catch (VerificationException ex) {
            // expected case: fail on missing org.eclipse.osgi.source
            ex.printStackTrace();
        }
    }

    @Test
    public void testWithoutIncludeSourcesButForced() throws Exception {
        Verifier verifier = getVerifier("/target.includeSources", false);
        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        // TODO switch from target-platform-configuration
        verifier.executeGoals(Arrays.asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testIncludeSourceDefault() throws Exception {
        Verifier verifier = getVerifier("/target.includeSources", false);
        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        TargetDefinitionUtil.setIncludeSource(platformFile, true);
        verifier.executeGoals(Arrays.asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testIncludeSourceIgnore() throws Exception {
        Verifier verifier = getVerifier("/target.includeSources", false);
        File platformFile = new File(verifier.getBasedir(), "platform.target");
        TargetDefinitionUtil.setRepositoryURLs(platformFile, ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        TargetDefinitionUtil.setIncludeSource(platformFile, true);
        // TODO switch from target-platform-configuration
        try {
            verifier.executeGoals(Arrays.asList("clean", "verify"));
        } catch (VerificationException ex) {
            // expected case: fail on missing org.eclipse.osgi.source
            ex.printStackTrace();
        }
    }
}
