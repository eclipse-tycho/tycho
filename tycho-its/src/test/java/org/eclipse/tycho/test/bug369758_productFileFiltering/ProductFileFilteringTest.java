/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug369758_productFileFiltering;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.IOUtil;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class ProductFileFilteringTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/369758_productFileFiltering", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        Properties p = new Properties();

        String basedir = verifier.getBasedir();
        InputStream is = new BufferedInputStream(new FileInputStream(new File(basedir,
                "target/products/369758_productFileFiltering/linux/gtk/x86/configuration/config.ini")));
        try {
            p.load(is);
        } finally {
            IOUtil.close(is);
        }

        Assert.assertEquals("1.0.0.123abc", p.getProperty("eclipse.buildId"));
    }
}
